package com.personal.toolkit.todo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.toolkit.todo.dto.TodoQueryRequest;
import com.personal.toolkit.todo.dto.TodoItemRequest;
import com.personal.toolkit.todo.dto.PageResponse;
import com.personal.toolkit.todo.dto.TodoOptionResponse;
import com.personal.toolkit.todo.dto.TodoSubItemSummaryResponse;
import com.personal.toolkit.todo.entity.TodoItem;
import com.personal.toolkit.todo.repository.TodoRepository;
import com.personal.toolkit.todo.repository.TodoSubItemRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 处理待办事项的核心业务逻辑，并以 Cache-Aside 模式协调 Oracle 与 Redis 的读写。
 */
@Service
public class TodoService {

    private static final Logger log = LoggerFactory.getLogger(TodoService.class);
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);
    private static final Set<String> ALLOWED_STATUSES = Set.of("PENDING", "DONE");
    private static final Set<String> ALLOWED_RECURRENCE_TYPES = Set.of("NONE", "DAILY", "WEEKLY", "MONTHLY");

    private final TodoRepository todoRepository;
    private final TodoSubItemRepository todoSubItemRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    public TodoService(TodoRepository todoRepository,
                       TodoSubItemRepository todoSubItemRepository,
                       RedisTemplate<String, Object> redisTemplate,
                       ObjectMapper objectMapper) {
        this.todoRepository = todoRepository;
        this.todoSubItemRepository = todoSubItemRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 按筛选条件分页查询待办事项列表，并按传入分页参数返回分页元数据。
     *
     * @param queryRequest 列表筛选条件
     * @param page 页码，从 0 开始
     * @param size 每页条数
     * @param sort 排序规则
     * @return 包含分页数据和分页元信息的响应对象
     */
    @Transactional(readOnly = true)
    public PageResponse<TodoItem> findAll(TodoQueryRequest queryRequest, int page, int size, Sort sort) {
        Pageable pageable = PageRequest.of(page, size, sort);
        Page<TodoItem> todoPage = todoRepository.findAll(buildSpecification(queryRequest), pageable);
        hydrateSummaries(todoPage.getContent());
        return PageResponse.from(todoPage);
    }

    /**
     * 查询当前可用的分类与标签候选项，用于前端管理和下拉选择。
     *
     * @return 分类与标签选项集合
     */
    @Transactional(readOnly = true)
    public TodoOptionResponse getOptions() {
        List<TodoItem> todoItems = todoRepository.findAll(buildSpecification(defaultActiveQuery()));

        List<String> categories = todoItems.stream()
                .map(TodoItem::getCategory)
                .filter(this::hasText)
                .map(String::trim)
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());

        List<String> tags = todoItems.stream()
                .map(TodoItem::getTags)
                .filter(this::hasText)
                .flatMap(value -> List.of(value.split(",")).stream())
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .sorted(String::compareToIgnoreCase)
                .collect(Collectors.toList());

        return new TodoOptionResponse(categories, tags);
    }

    /**
     * 根据主键查询单个待办事项，优先读取对象缓存，缓存未命中时再访问数据库并回填缓存。
     *
     * @param id 待办事项主键
     * @return 对应的待办事项实体
     */
    @Transactional(readOnly = true)
    public TodoItem findById(Long id) {
        try {
            Object cachedValue = redisTemplate.opsForValue().get(todoItemCacheKey(id));
            if (cachedValue != null) {
                TodoItem cachedTodoItem = objectMapper.convertValue(cachedValue, TodoItem.class);
                cacheTodoItemWithSummarySafely(cachedTodoItem);
                return cachedTodoItem;
            }
        } catch (RuntimeException ex) {
            log.warn("Failed to read todo item cache for id={}, falling back to Oracle", id, ex);
        }

        TodoItem todoItem = getExistingTodoItem(id);
        cacheTodoItemWithSummarySafely(todoItem);
        return todoItem;
    }

    /**
     * 创建新的待办事项，并在事务提交后刷新相关缓存。
     *
     * @param request 待办事项创建请求
     * @return 已持久化的待办事项实体
     */
    @Transactional
    public TodoItem create(TodoItemRequest request) {
        TodoItem todoItem = new TodoItem();
        applyRequest(todoItem, request);
        if ("DONE".equals(todoItem.getStatus())) {
            todoItem.setCompletedAt(LocalDateTime.now());
        }

        TodoItem savedTodoItem = todoRepository.save(todoItem);
        runAfterCommit(() -> {
            cacheTodoItemWithSummarySafely(savedTodoItem);
        });
        return savedTodoItem;
    }

    /**
     * 更新指定主键的待办事项，并在事务提交后刷新对象缓存和列表缓存。
     *
     * @param id 待办事项主键
     * @param request 待办事项更新请求
     * @return 已更新的待办事项实体
     */
    @Transactional
    public TodoItem update(Long id, TodoItemRequest request) {
        TodoItem todoItem = getExistingTodoItem(id);
        String previousStatus = todoItem.getStatus();
        applyRequest(todoItem, request);
        if (isTransitionToDone(previousStatus, todoItem.getStatus())) {
            markAsCompleted(todoItem, LocalDateTime.now());
        }

        TodoItem savedTodoItem = todoRepository.save(todoItem);
        List<TodoItem> generatedRecurringTodos = maybeGenerateRecurringTodos(List.of(savedTodoItem), Map.of(savedTodoItem.getId(), previousStatus));
        runAfterCommit(() -> {
            cacheTodoItemWithSummarySafely(savedTodoItem);
            generatedRecurringTodos.forEach(this::cacheTodoItemWithSummarySafely);
        });
        return savedTodoItem;
    }

    /**
     * 删除指定主键的待办事项，并在事务提交后清理相关缓存。
     *
     * @param id 待办事项主键
     */
    @Transactional
    public void delete(Long id) {
        softDelete(id);
    }

    /**
     * 批量将待办事项标记为已完成。
     *
     * @param ids 待办事项主键集合
     * @return 批量更新后的待办事项列表
     */
    @Transactional
    public List<TodoItem> batchComplete(List<Long> ids) {
        List<TodoItem> todoItems = getExistingTodoItems(ids, false);
        Map<Long, String> previousStatuses = todoItems.stream()
                .collect(Collectors.toMap(TodoItem::getId, TodoItem::getStatus));
        LocalDateTime completionTime = LocalDateTime.now();
        todoItems.forEach(todoItem -> markAsCompleted(todoItem, completionTime));

        List<TodoItem> savedItems = todoRepository.saveAll(todoItems);
        List<TodoItem> generatedRecurringTodos = maybeGenerateRecurringTodos(savedItems, previousStatuses);
        runAfterCommit(() -> {
            savedItems.forEach(this::cacheTodoItemWithSummarySafely);
            generatedRecurringTodos.forEach(this::cacheTodoItemWithSummarySafely);
        });
        return savedItems;
    }

    /**
     * 批量软删除待办事项，将其移动到回收站。
     *
     * @param ids 待办事项主键集合
     */
    @Transactional
    public void batchDelete(List<Long> ids) {
        List<TodoItem> todoItems = getExistingTodoItems(ids, false);
        LocalDateTime deletedAt = LocalDateTime.now();
        todoItems.forEach(todoItem -> todoItem.setDeletedAt(deletedAt));

        todoRepository.saveAll(todoItems);
        runAfterCommit(() -> todoItems.forEach(todoItem -> evictTodoItemCacheSafely(todoItem.getId())));
    }

    /**
     * 恢复单个回收站中的待办事项。
     *
     * @param id 待办事项主键
     * @return 恢复后的待办事项实体
     */
    @Transactional
    public TodoItem restore(Long id) {
        TodoItem todoItem = getExistingTodoItem(id, true);
        if (todoItem.getDeletedAt() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Todo item is not in recycle bin: " + id);
        }

        todoItem.setDeletedAt(null);
        TodoItem savedTodoItem = todoRepository.save(todoItem);
        runAfterCommit(() -> cacheTodoItemWithSummarySafely(savedTodoItem));
        return savedTodoItem;
    }

    /**
     * 批量恢复回收站中的待办事项。
     *
     * @param ids 待办事项主键集合
     * @return 恢复后的待办事项列表
     */
    @Transactional
    public List<TodoItem> batchRestore(List<Long> ids) {
        List<TodoItem> todoItems = getExistingTodoItems(ids, true).stream()
                .filter(todoItem -> todoItem.getDeletedAt() != null)
                .collect(Collectors.toList());

        if (todoItems.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "No deleted todo items to restore");
        }

        todoItems.forEach(todoItem -> todoItem.setDeletedAt(null));
        List<TodoItem> savedItems = todoRepository.saveAll(todoItems);
        runAfterCommit(() -> savedItems.forEach(this::cacheTodoItemWithSummarySafely));
        return savedItems;
    }

    /**
     * 执行单个待办事项的软删除，将其移动到回收站。
     *
     * @param id 待办事项主键
     */
    @Transactional
    public void softDelete(Long id) {
        TodoItem todoItem = getExistingTodoItem(id);
        todoItem.setDeletedAt(LocalDateTime.now());
        todoRepository.save(todoItem);
        runAfterCommit(() -> evictTodoItemCacheSafely(id));
    }

    /**
     * 查询数据库中的待办事项，不存在时抛出 404 异常。
     *
     * @param id 待办事项主键
     * @return 已存在的待办事项实体
     */
    private TodoItem getExistingTodoItem(Long id) {
        return todoRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo item not found: " + id));
    }

    /**
     * 查询数据库中的待办事项，可根据是否允许已删除数据决定查询范围。
     *
     * @param id 待办事项主键
     * @param includeDeleted 是否允许返回已删除数据
     * @return 已存在的待办事项实体
     */
    private TodoItem getExistingTodoItem(Long id, boolean includeDeleted) {
        return (includeDeleted ? todoRepository.findById(id) : todoRepository.findByIdAndDeletedAtIsNull(id))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo item not found: " + id));
    }

    /**
     * 根据主键集合查询待办事项列表，并校验是否存在缺失记录。
     *
     * @param ids 待办事项主键集合
     * @param includeDeleted 是否允许返回已删除数据
     * @return 查询到的待办事项列表
     */
    private List<TodoItem> getExistingTodoItems(List<Long> ids, boolean includeDeleted) {
        if (ids == null || ids.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ids must not be empty");
        }

        List<TodoItem> todoItems = todoRepository.findAllByIdIn(ids).stream()
                .filter(todoItem -> includeDeleted || todoItem.getDeletedAt() == null)
                .sorted(Comparator.comparing(TodoItem::getId))
                .collect(Collectors.toList());

        if (todoItems.size() != ids.size()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Some todo items were not found");
        }

        return todoItems;
    }

    /**
     * 将请求体中的标题、状态、优先级、分类和标签规范化后写入实体，并校验关键字段合法性。
     *
     * @param todoItem 待填充的待办事项实体
     * @param request 客户端提交的请求体
     */
    private void applyRequest(TodoItem todoItem, TodoItemRequest request) {
        String normalizedTitle = request.getTitle().trim();
        String normalizedStatus = request.getStatus().trim().toUpperCase(Locale.ROOT);
        String normalizedRecurrenceType = normalizeRecurrenceType(request.getRecurrenceType());
        int normalizedRecurrenceInterval = normalizeRecurrenceInterval(request.getRecurrenceInterval());
        LocalDateTime recurrenceEndTime = request.getRecurrenceEndTime();

        if (!ALLOWED_STATUSES.contains(normalizedStatus)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "status must be one of: " + ALLOWED_STATUSES
            );
        }

        if (request.getPriority() < 1 || request.getPriority() > 5) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "priority must be between 1 and 5");
        }

        if (!"NONE".equals(normalizedRecurrenceType) && request.getDueDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "dueDate must not be null when recurrenceType is enabled");
        }

        if (recurrenceEndTime != null && request.getDueDate() != null && recurrenceEndTime.isBefore(request.getDueDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recurrenceEndTime must be after or equal to dueDate");
        }

        todoItem.setTitle(normalizedTitle);
        todoItem.setStatus(normalizedStatus);
        todoItem.setPriority(request.getPriority());
        todoItem.setDueDate(request.getDueDate());
        todoItem.setCategory(normalizeNullableText(request.getCategory()));
        todoItem.setTags(normalizeTags(request.getTags()));
        todoItem.setRecurrenceType(normalizedRecurrenceType);
        todoItem.setRecurrenceInterval(normalizedRecurrenceInterval);
        todoItem.setRecurrenceEndTime("NONE".equals(normalizedRecurrenceType) ? null : recurrenceEndTime);
        todoItem.setNextTriggerTime("NONE".equals(normalizedRecurrenceType) ? null : request.getDueDate());
        if (!"DONE".equals(normalizedStatus)) {
            todoItem.setCompletedAt(null);
        }
    }

    /**
     * 在任务首次变为完成状态时写入完成时间，避免重复完成产生新的时间漂移。
     *
     * @param todoItem 目标待办事项
     * @param completionTime 完成时间
     */
    private void markAsCompleted(TodoItem todoItem, LocalDateTime completionTime) {
        todoItem.setStatus("DONE");
        todoItem.setCompletedAt(completionTime);
    }

    /**
     * 针对本次由未完成变为已完成的重复任务生成下一条待办实例。
     *
     * @param completedTodos 已保存的完成任务列表
     * @param previousStatuses 保存前的状态映射
     * @return 新生成的重复任务列表
     */
    private List<TodoItem> maybeGenerateRecurringTodos(List<TodoItem> completedTodos, Map<Long, String> previousStatuses) {
        if (completedTodos == null || completedTodos.isEmpty()) {
            return List.of();
        }

        List<TodoItem> nextRecurringTodos = completedTodos.stream()
                .filter(todoItem -> isTransitionToDone(previousStatuses.get(todoItem.getId()), todoItem.getStatus()))
                .map(this::buildNextRecurringTodo)
                .filter(java.util.Objects::nonNull)
                .filter(nextTodo -> !hasMatchingRecurringSuccessor(nextTodo))
                .toList();

        if (nextRecurringTodos.isEmpty()) {
            return List.of();
        }

        return todoRepository.saveAll(nextRecurringTodos);
    }

    /**
     * 根据已完成的重复任务构造下一条待办实例，若达到结束时间则不再生成。
     *
     * @param completedTodo 已完成的重复任务
     * @return 下一条待办实例；若不应继续生成则返回 null
     */
    private TodoItem buildNextRecurringTodo(TodoItem completedTodo) {
        if (completedTodo == null || !isRecurring(completedTodo)) {
            return null;
        }

        LocalDateTime currentTriggerTime = resolveRecurrenceAnchor(completedTodo);
        LocalDateTime nextTriggerTime = calculateNextTriggerTime(
                currentTriggerTime,
                completedTodo.getRecurrenceType(),
                completedTodo.getRecurrenceInterval()
        );

        if (completedTodo.getRecurrenceEndTime() != null && nextTriggerTime.isAfter(completedTodo.getRecurrenceEndTime())) {
            return null;
        }

        TodoItem nextTodo = new TodoItem();
        nextTodo.setTitle(completedTodo.getTitle());
        nextTodo.setStatus("PENDING");
        nextTodo.setPriority(completedTodo.getPriority());
        nextTodo.setDueDate(nextTriggerTime);
        nextTodo.setCategory(completedTodo.getCategory());
        nextTodo.setTags(completedTodo.getTags());
        nextTodo.setRecurrenceType(completedTodo.getRecurrenceType());
        nextTodo.setRecurrenceInterval(completedTodo.getRecurrenceInterval());
        nextTodo.setRecurrenceEndTime(completedTodo.getRecurrenceEndTime());
        nextTodo.setNextTriggerTime(nextTriggerTime);
        nextTodo.setCompletedAt(null);
        nextTodo.setDeletedAt(null);
        return nextTodo;
    }

    /**
     * 判断当前计划中的下一条重复任务是否已经存在，避免任务被重新打开后再次完成时生成重复实例。
     *
     * @param nextTodo 候选的下一条重复任务实例
     * @return 若数据库中已存在同一计划实例则返回 true
     */
    private boolean hasMatchingRecurringSuccessor(TodoItem nextTodo) {
        if (nextTodo == null) {
            return false;
        }

        return todoRepository.existsByDeletedAtIsNullAndTitleAndStatusAndPriorityAndDueDateAndCategoryAndTagsAndRecurrenceTypeAndRecurrenceIntervalAndRecurrenceEndTimeAndNextTriggerTime(
                nextTodo.getTitle(),
                nextTodo.getStatus(),
                nextTodo.getPriority(),
                nextTodo.getDueDate(),
                nextTodo.getCategory(),
                nextTodo.getTags(),
                nextTodo.getRecurrenceType(),
                nextTodo.getRecurrenceInterval(),
                nextTodo.getRecurrenceEndTime(),
                nextTodo.getNextTriggerTime()
        );
    }

    /**
     * 判断任务是否从未完成状态首次切换为已完成状态。
     *
     * @param previousStatus 更新前状态
     * @param currentStatus 更新后状态
     * @return 是否属于首次完成
     */
    private boolean isTransitionToDone(String previousStatus, String currentStatus) {
        return "DONE".equals(currentStatus) && !"DONE".equals(previousStatus);
    }

    /**
     * 判断任务是否配置了重复规则。
     *
     * @param todoItem 目标待办事项
     * @return 是否为重复任务
     */
    private boolean isRecurring(TodoItem todoItem) {
        return todoItem != null && hasText(todoItem.getRecurrenceType()) && !"NONE".equals(todoItem.getRecurrenceType());
    }

    /**
     * 解析重复任务当前这一条实例的计划触发时间，优先使用 nextTriggerTime，其次使用 dueDate。
     * nextTriggerTime 在当前实现中表示“本条实例的计划触发/到期时间锚点”，用于推算下一条实例，
     * 而不是直接表示“再下一条实例”的时间。
     *
     * @param todoItem 重复任务
     * @return 当前触发锚点时间
     */
    private LocalDateTime resolveRecurrenceAnchor(TodoItem todoItem) {
        if (todoItem.getNextTriggerTime() != null) {
            return todoItem.getNextTriggerTime();
        }
        if (todoItem.getDueDate() != null) {
            return todoItem.getDueDate();
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Recurring todo must have dueDate or nextTriggerTime");
    }

    /**
     * 根据重复规则计算下一次触发时间。
     *
     * @param currentTriggerTime 当前触发时间
     * @param recurrenceType 重复类型
     * @param recurrenceInterval 重复间隔
     * @return 下一次触发时间
     */
    private LocalDateTime calculateNextTriggerTime(LocalDateTime currentTriggerTime,
                                                   String recurrenceType,
                                                   Integer recurrenceInterval) {
        int interval = recurrenceInterval == null ? 1 : recurrenceInterval;
        return switch (recurrenceType) {
            case "DAILY" -> currentTriggerTime.plusDays(interval);
            case "WEEKLY" -> currentTriggerTime.plusWeeks(interval);
            case "MONTHLY" -> currentTriggerTime.plusMonths(interval);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported recurrenceType: " + recurrenceType);
        };
    }

    /**
     * 规范化重复任务类型，空值时默认 NONE。
     *
     * @param recurrenceType 原始重复任务类型
     * @return 规范化后的重复任务类型
     */
    private String normalizeRecurrenceType(String recurrenceType) {
        if (!hasText(recurrenceType)) {
            return "NONE";
        }

        String normalizedRecurrenceType = recurrenceType.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_RECURRENCE_TYPES.contains(normalizedRecurrenceType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recurrenceType must be one of: " + ALLOWED_RECURRENCE_TYPES);
        }
        return normalizedRecurrenceType;
    }

    /**
     * 规范化重复任务间隔，空值时默认 1。
     *
     * @param recurrenceInterval 原始重复任务间隔
     * @return 规范化后的重复任务间隔
     */
    private Integer normalizeRecurrenceInterval(Integer recurrenceInterval) {
        int normalizedRecurrenceInterval = recurrenceInterval == null ? 1 : recurrenceInterval;
        if (normalizedRecurrenceInterval < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recurrenceInterval must be greater than or equal to 1");
        }
        return normalizedRecurrenceInterval;
    }

    /**
     * 安全写入单个待办事项缓存，出现 Redis 异常时仅记录日志而不影响主流程。
     *
     * @param todoItem 需要缓存的待办事项实体
     */
    private void cacheTodoItemSafely(TodoItem todoItem) {
        try {
            redisTemplate.opsForValue().set(todoItemCacheKey(todoItem.getId()), todoItem, CACHE_TTL);
        } catch (RuntimeException ex) {
            log.warn("Failed to write todo item cache for id={}", todoItem.getId(), ex);
        }
    }

    /**
     * 在写入待办详情缓存前先补齐最新 checklist 摘要，避免缓存中的聚合字段为空或过期。
     *
     * @param todoItem 需要补齐摘要并缓存的待办事项实体
     */
    private void cacheTodoItemWithSummarySafely(TodoItem todoItem) {
        if (todoItem == null || todoItem.getId() == null) {
            return;
        }

        hydrateSummary(todoItem);
        cacheTodoItemSafely(todoItem);
    }

    /**
     * 安全删除指定待办事项对象缓存，避免 Redis 故障影响主业务流程。
     *
     * @param id 待办事项主键
     */
    private void evictTodoItemCacheSafely(Long id) {
        try {
            redisTemplate.delete(todoItemCacheKey(id));
        } catch (RuntimeException ex) {
            log.warn("Failed to evict todo item cache for id={}", id, ex);
        }
    }

    /**
     * 生成单个待办事项缓存键。
     *
     * @param id 待办事项主键
     * @return Redis 中使用的对象缓存键
     */
    private String todoItemCacheKey(Long id) {
        return TodoCacheKeys.todoItem(id);
    }

    /**
     * 为待办事项列表批量补充子任务摘要，避免前端逐条单独查询进度。
     *
     * @param todoItems 当前页待办事项列表
     */
    private void hydrateSummaries(List<TodoItem> todoItems) {
        if (todoItems == null || todoItems.isEmpty()) {
            return;
        }

        List<Long> todoIds = todoItems.stream().map(TodoItem::getId).toList();
        Map<Long, TodoSubItemSummaryResponse> summaryMap = buildSummaryMap(todoIds);
        todoItems.forEach(todoItem -> todoItem.setSubItemSummary(
                summaryMap.getOrDefault(todoItem.getId(), new TodoSubItemSummaryResponse(0, 0, 0))
        ));
    }

    /**
     * 为单个待办事项补充 checklist 摘要信息。
     *
     * @param todoItem 待补充摘要的待办事项
     */
    private void hydrateSummary(TodoItem todoItem) {
        if (todoItem == null || todoItem.getId() == null) {
            return;
        }
        Map<Long, TodoSubItemSummaryResponse> summaryMap = buildSummaryMap(List.of(todoItem.getId()));
        todoItem.setSubItemSummary(summaryMap.getOrDefault(todoItem.getId(), new TodoSubItemSummaryResponse(0, 0, 0)));
    }

    /**
     * 按主任务主键集合聚合子任务完成情况，供列表页和详情页复用。
     *
     * @param todoIds 主任务主键集合
     * @return 以主任务主键分组的摘要映射
     */
    private Map<Long, TodoSubItemSummaryResponse> buildSummaryMap(List<Long> todoIds) {
        Map<Long, TodoSubItemSummaryResponse> summaryMap = new HashMap<>();
        if (todoIds == null || todoIds.isEmpty()) {
            return summaryMap;
        }

        List<Object[]> rows = todoSubItemRepository.summarizeByTodoIds(todoIds);
        if (rows == null || rows.isEmpty()) {
            return summaryMap;
        }

        rows.forEach(row -> {
            Long todoId = (Long) row[0];
            int totalCount = ((Long) row[1]).intValue();
            int completedCount = row[2] == null ? 0 : ((Long) row[2]).intValue();
            int progressPercent = totalCount == 0 ? 0 : (completedCount * 100) / totalCount;
            summaryMap.put(todoId, new TodoSubItemSummaryResponse(totalCount, completedCount, progressPercent));
        });

        return summaryMap;
    }

    /**
     * 根据查询条件构建动态筛选规则，用于支持状态、优先级、分类、标签、关键字和截止时间区间查询。
     *
     * @param queryRequest 列表筛选条件
     * @return JPA Specification 查询条件对象
     */
    private Specification<TodoItem> buildSpecification(TodoQueryRequest queryRequest) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (hasText(queryRequest.getStatus())) {
                predicates.add(criteriaBuilder.equal(
                        root.get("status"),
                        queryRequest.getStatus().trim().toUpperCase(Locale.ROOT)
                ));
            }

            if (queryRequest.getPriority() != null) {
                predicates.add(criteriaBuilder.equal(root.get("priority"), queryRequest.getPriority()));
            }

            if (hasText(queryRequest.getCategory())) {
                predicates.add(criteriaBuilder.equal(
                        criteriaBuilder.lower(root.get("category")),
                        queryRequest.getCategory().trim().toLowerCase(Locale.ROOT)
                ));
            }

            if (hasText(queryRequest.getKeyword())) {
                String keyword = "%" + queryRequest.getKeyword().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("title")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("category")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("tags")), keyword)
                ));
            }

            if (hasText(queryRequest.getTag())) {
                String tag = "%" + queryRequest.getTag().trim().toLowerCase(Locale.ROOT) + "%";
                predicates.add(criteriaBuilder.like(criteriaBuilder.lower(root.get("tags")), tag));
            }

            if (queryRequest.getDueDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("dueDate"),
                        queryRequest.getDueDateFrom().atStartOfDay()
                ));
            }

            if (queryRequest.getDueDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("dueDate"),
                        queryRequest.getDueDateTo().atTime(23, 59, 59)
                ));
            }

            boolean includeDeleted = Boolean.TRUE.equals(queryRequest.getIncludeDeleted());
            if (includeDeleted) {
                predicates.add(criteriaBuilder.isNotNull(root.get("deletedAt")));
            } else {
                predicates.add(criteriaBuilder.isNull(root.get("deletedAt")));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    /**
     * 创建默认查询活动待办的筛选条件对象。
     *
     * @return 默认筛选条件对象
     */
    private TodoQueryRequest defaultActiveQuery() {
        TodoQueryRequest queryRequest = new TodoQueryRequest();
        queryRequest.setIncludeDeleted(false);
        return queryRequest;
    }

    /**
     * 判断字符串是否包含有效文本内容。
     *
     * @param value 待判断字符串
     * @return 是否包含非空白内容
     */
    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * 规范化可为空的文本字段，空白内容将被转换为 null。
     *
     * @param value 原始文本值
     * @return 规范化后的文本值
     */
    private String normalizeNullableText(String value) {
        if (!hasText(value)) {
            return null;
        }
        return value.trim();
    }

    /**
     * 规范化标签字符串，去除首尾空白、空标签和重复标签，最终以逗号拼接存储。
     *
     * @param tags 原始标签字符串
     * @return 规范化后的标签字符串
     */
    private String normalizeTags(String tags) {
        if (!hasText(tags)) {
            return null;
        }

        List<String> normalizedTags = List.of(tags.split(","))
                .stream()
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .collect(Collectors.toList());

        if (normalizedTags.isEmpty()) {
            return null;
        }

        return String.join(",", normalizedTags);
    }

    /**
     * 在当前事务成功提交后执行指定操作，用于保证缓存更新只发生在数据库提交成功之后。
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
}
