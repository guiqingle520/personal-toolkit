package com.personal.toolkit.todo.service;

import com.personal.toolkit.auth.security.CurrentUserProvider;
import com.personal.toolkit.todo.dto.TodoSubItemRequest;
import com.personal.toolkit.todo.dto.TodoSubItemResponse;
import com.personal.toolkit.todo.dto.TodoSubItemSummaryResponse;
import com.personal.toolkit.todo.entity.TodoSubItem;
import com.personal.toolkit.todo.repository.TodoRepository;
import com.personal.toolkit.todo.repository.TodoSubItemRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Locale;

/**
 * 处理子任务 checklist 的新增、更新、删除与进度汇总逻辑。
 */
@Service
public class TodoSubItemService {

    private static final Logger log = LoggerFactory.getLogger(TodoSubItemService.class);

    private final TodoSubItemRepository todoSubItemRepository;
    private final TodoRepository todoRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final CurrentUserProvider currentUserProvider;

    public TodoSubItemService(TodoSubItemRepository todoSubItemRepository,
                              TodoRepository todoRepository,
                              RedisTemplate<String, Object> redisTemplate,
                              CurrentUserProvider currentUserProvider) {
        this.todoSubItemRepository = todoSubItemRepository;
        this.todoRepository = todoRepository;
        this.redisTemplate = redisTemplate;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * 查询指定主任务下的所有子任务列表，并按排序值返回。
     *
     * @param todoId 主任务主键
     * @return 子任务响应列表
     */
    @Transactional(readOnly = true)
    public List<TodoSubItemResponse> findByTodoId(Long todoId) {
        validateParentTodoExists(todoId);
        return todoSubItemRepository.findAllByTodoIdOrderBySortOrderAscIdAsc(todoId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * 在指定主任务下创建新的子任务记录。
     *
     * @param todoId 主任务主键
     * @param request 子任务请求体
     * @return 新建后的子任务响应
     */
    @Transactional
    public TodoSubItemResponse create(Long todoId, TodoSubItemRequest request) {
        validateParentTodoExists(todoId);

        TodoSubItem entity = new TodoSubItem();
        entity.setTodoId(todoId);
        entity.setTitle(request.getTitle().trim());
        entity.setStatus(normalizeStatus(request.getStatus()));
        entity.setSortOrder(normalizeSortOrder(request.getSortOrder()));

        TodoSubItem savedEntity = todoSubItemRepository.save(entity);
        runAfterCommit(() -> evictParentTodoCacheSafely(todoId));
        return toResponse(savedEntity);
    }

    /**
     * 更新指定主任务下的单条子任务记录。
     *
     * @param todoId 主任务主键
     * @param subItemId 子任务主键
     * @param request 子任务请求体
     * @return 更新后的子任务响应
     */
    @Transactional
    public TodoSubItemResponse update(Long todoId, Long subItemId, TodoSubItemRequest request) {
        TodoSubItem entity = getExistingSubItem(todoId, subItemId);
        entity.setTitle(request.getTitle().trim());
        entity.setStatus(normalizeStatus(request.getStatus()));
        entity.setSortOrder(normalizeSortOrder(request.getSortOrder()));
        TodoSubItem savedEntity = todoSubItemRepository.save(entity);
        runAfterCommit(() -> evictParentTodoCacheSafely(todoId));
        return toResponse(savedEntity);
    }

    /**
     * 删除指定主任务下的单条子任务记录。
     *
     * @param todoId 主任务主键
     * @param subItemId 子任务主键
     */
    @Transactional
    public void delete(Long todoId, Long subItemId) {
        TodoSubItem entity = getExistingSubItem(todoId, subItemId);
        todoSubItemRepository.delete(entity);
        runAfterCommit(() -> evictParentTodoCacheSafely(todoId));
    }

    /**
     * 统计指定主任务下子任务的总数、完成数和完成百分比。
     *
     * @param todoId 主任务主键
     * @return 子任务进度汇总
     */
    @Transactional(readOnly = true)
    public TodoSubItemSummaryResponse getSummary(Long todoId) {
        validateParentTodoExists(todoId);
        return buildSummary(todoSubItemRepository.findAllByTodoIdOrderBySortOrderAscIdAsc(todoId));
    }

    /**
     * 校验指定主任务是否存在且未被软删除。
     *
     * @param todoId 主任务主键
     */
    public void validateParentTodoExists(Long todoId) {
        Long userId = currentUserProvider.getCurrentUserId();
        if (!todoRepository.existsByIdAndUserIdAndDeletedAtIsNull(todoId, userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo item not found: " + todoId);
        }
    }

    /**
     * 查询指定主任务下的目标子任务，不存在时抛出 404 异常。
     *
     * @param todoId 主任务主键
     * @param subItemId 子任务主键
     * @return 已存在的子任务实体
     */
    private TodoSubItem getExistingSubItem(Long todoId, Long subItemId) {
        validateParentTodoExists(todoId);
        return todoSubItemRepository.findByIdAndTodoId(subItemId, todoId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo sub item not found: " + subItemId));
    }

    /**
     * 规范化子任务状态，默认使用 PENDING 并限制为允许值集合。
     *
     * @param status 原始状态值
     * @return 规范化后的状态值
     */
    private String normalizeStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return "PENDING";
        }

        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!List.of("PENDING", "DONE").contains(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be one of: [PENDING, DONE]");
        }
        return normalizedStatus;
    }

    /**
     * 规范化排序值，空值时使用 0 作为默认顺序。
     *
     * @param sortOrder 原始排序值
     * @return 规范化后的排序值
     */
    private Integer normalizeSortOrder(Integer sortOrder) {
        return sortOrder == null ? 0 : sortOrder;
    }

    /**
     * 将子任务实体映射为前端可消费的响应对象。
     *
     * @param entity 子任务实体
     * @return 子任务响应对象
     */
    private TodoSubItemResponse toResponse(TodoSubItem entity) {
        TodoSubItemResponse response = new TodoSubItemResponse();
        response.setId(entity.getId());
        response.setTodoId(entity.getTodoId());
        response.setTitle(entity.getTitle());
        response.setStatus(entity.getStatus());
        response.setSortOrder(entity.getSortOrder());
        response.setCreateTime(entity.getCreateTime());
        response.setUpdateTime(entity.getUpdateTime());
        return response;
    }

    /**
     * 基于子任务列表计算聚合进度信息。
     *
     * @param items 子任务列表
     * @return 子任务进度汇总
     */
    private TodoSubItemSummaryResponse buildSummary(List<TodoSubItem> items) {
        int totalCount = items.size();
        int completedCount = (int) items.stream().filter(item -> "DONE".equals(item.getStatus())).count();
        int progressPercent = totalCount == 0 ? 0 : (completedCount * 100) / totalCount;
        return new TodoSubItemSummaryResponse(totalCount, completedCount, progressPercent);
    }

    /**
     * 在当前事务提交后执行缓存动作，确保 Redis 与数据库提交顺序一致。
     *
     * @param action 事务提交后需要执行的动作
     */
    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            action.run();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                action.run();
            }
        });
    }

    /**
     * 安全驱逐父任务详情缓存，避免 checklist 变更后继续读取陈旧摘要。
     *
     * @param todoId 父任务主键
     */
    private void evictParentTodoCacheSafely(Long todoId) {
        try {
            redisTemplate.delete(todoItemCacheKey(todoId));
        } catch (RuntimeException ex) {
            log.warn("Failed to evict parent todo cache for todoId={}", todoId, ex);
        }
    }

    /**
     * 生成父任务详情缓存键，与 TodoService 保持一致。
     *
     * @param todoId 父任务主键
     * @return Redis 中使用的父任务缓存键
     */
    private String todoItemCacheKey(Long todoId) {
        return TodoCacheKeys.todoItem(currentUserProvider.getCurrentUserId(), todoId);
    }
}
