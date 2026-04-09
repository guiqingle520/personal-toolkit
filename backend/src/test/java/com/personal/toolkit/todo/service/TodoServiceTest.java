package com.personal.toolkit.todo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.toolkit.todo.dto.TodoItemRequest;
import com.personal.toolkit.todo.dto.TodoOptionResponse;
import com.personal.toolkit.todo.dto.TodoSubItemRequest;
import com.personal.toolkit.todo.dto.TodoSubItemSummaryResponse;
import com.personal.toolkit.todo.entity.TodoItem;
import com.personal.toolkit.todo.repository.TodoRepository;
import com.personal.toolkit.todo.repository.TodoSubItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 TodoService 的批量操作、软删除与候选项聚合行为。
 */
@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private TodoSubItemRepository todoSubItemRepository;

    private TodoService todoService;

    @BeforeEach
    void setUp() {
        todoService = new TodoService(todoRepository, todoSubItemRepository, redisTemplate, new ObjectMapper());
    }

    /**
     * 缓存命中详情时也应补齐最新摘要并刷新缓存，避免旧缓存中的聚合字段为空或过期。
     */
    @Test
    void findByIdShouldHydrateCachedTodoSummaryAndRefreshCache() {
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        TodoItem cachedTodo = createTodo(9L, "PENDING", null);
        cachedTodo.setSubItemSummary(null);
        List<Object[]> summaryRows = List.<Object[]>of(new Object[]{9L, 4L, 1L});

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(TodoCacheKeys.todoItem(9L))).thenReturn(cachedTodo);
        when(todoSubItemRepository.summarizeByTodoIds(List.of(9L))).thenReturn(summaryRows);

        TodoItem result = todoService.findById(9L);

        assertEquals(4, result.getSubItemSummary().getTotalCount());
        assertEquals(1, result.getSubItemSummary().getCompletedCount());
        assertEquals(25, result.getSubItemSummary().getProgressPercent());
        verify(valueOperations).set(anyString(), any(TodoItem.class), any());
        verify(todoRepository, never()).findByIdAndDeletedAtIsNull(9L);
    }

    /**
     * 创建待办事项时应规范化分类与标签，并写入默认缓存。
     */
    @Test
    void createShouldNormalizeCategoryAndTags() {
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        TodoItemRequest request = new TodoItemRequest();
        request.setTitle("  Write tests  ");
        request.setStatus("pending");
        request.setPriority(4);
        request.setCategory("  Work  ");
        request.setTags(" urgent, backend ,urgent ");

        TodoItem saved = new TodoItem();
        saved.setId(1L);
        saved.setTitle("Write tests");
        saved.setStatus("PENDING");
        saved.setPriority(4);
        saved.setCategory("Work");
        saved.setTags("urgent,backend");
        saved.setRecurrenceType("NONE");
        saved.setRecurrenceInterval(1);

        when(todoRepository.save(any(TodoItem.class))).thenReturn(saved);

        TodoItem result = todoService.create(request);

        assertEquals("Write tests", result.getTitle());
        assertEquals("PENDING", result.getStatus());
        assertEquals("Work", result.getCategory());
        assertEquals("urgent,backend", result.getTags());
        assertEquals("NONE", result.getRecurrenceType());
        assertEquals(1, result.getRecurrenceInterval());
        verify(valueOperations).set(anyString(), any(TodoItem.class), any());
    }

    /**
     * 重复任务创建时应写入重复规则并基于 dueDate 初始化 nextTriggerTime。
     */
    @Test
    void createShouldInitializeRecurrenceFields() {
        TodoItemRequest request = new TodoItemRequest();
        request.setTitle("Daily sync");
        request.setStatus("PENDING");
        request.setPriority(3);
        request.setDueDate(LocalDateTime.of(2026, 4, 10, 9, 0));
        request.setRecurrenceType("daily");
        request.setRecurrenceInterval(2);
        request.setRecurrenceEndTime(LocalDateTime.of(2026, 5, 1, 9, 0));

        when(todoRepository.save(any(TodoItem.class))).thenAnswer(invocation -> {
            TodoItem entity = invocation.getArgument(0);
            entity.setId(18L);
            return entity;
        });

        TodoItem result = todoService.create(request);

        assertEquals("DAILY", result.getRecurrenceType());
        assertEquals(2, result.getRecurrenceInterval());
        assertEquals(LocalDateTime.of(2026, 4, 10, 9, 0), result.getNextTriggerTime());
        assertEquals(LocalDateTime.of(2026, 5, 1, 9, 0), result.getRecurrenceEndTime());
    }

    /**
     * 非法优先级应被拒绝，避免脏数据写入数据库。
     */
    @Test
    void createShouldRejectInvalidPriority() {
        TodoItemRequest request = new TodoItemRequest();
        request.setTitle("Bad priority");
        request.setStatus("PENDING");
        request.setPriority(8);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> todoService.create(request));

        assertEquals(400, exception.getStatusCode().value());
        verify(todoRepository, never()).save(any(TodoItem.class));
    }

    /**
     * 开启重复任务时若缺少 dueDate 应拒绝保存，因为系统无法推算下一次触发时间。
     */
    @Test
    void createShouldRejectRecurringTodoWithoutDueDate() {
        TodoItemRequest request = new TodoItemRequest();
        request.setTitle("Missing anchor");
        request.setStatus("PENDING");
        request.setPriority(3);
        request.setRecurrenceType("WEEKLY");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> todoService.create(request));

        assertEquals(400, exception.getStatusCode().value());
        assertEquals("dueDate must not be null when recurrenceType is enabled", exception.getReason());
    }

    /**
     * 批量完成应将所有目标任务状态更新为 DONE。
     */
    @Test
    void batchCompleteShouldMarkTodosDone() {
        TodoItem todo1 = createTodo(1L, "PENDING", null);
        TodoItem todo2 = createTodo(2L, "PENDING", null);

        when(todoRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(todo1, todo2));
        when(todoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<TodoItem> result = todoService.batchComplete(List.of(1L, 2L));

        assertEquals(List.of("DONE", "DONE"), result.stream().map(TodoItem::getStatus).toList());
    }

    /**
     * 批量完成重复任务时应仅为首次完成的任务生成下一条待办实例。
     */
    @Test
    void batchCompleteShouldGenerateNextRecurringTodoOnlyOncePerTransition() {
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        TodoItem recurringTodo = createTodo(21L, "PENDING", null);
        recurringTodo.setDueDate(LocalDateTime.of(2026, 4, 10, 9, 0));
        recurringTodo.setRecurrenceType("DAILY");
        recurringTodo.setRecurrenceInterval(1);
        recurringTodo.setNextTriggerTime(LocalDateTime.of(2026, 4, 10, 9, 0));

        TodoItem doneTodo = createTodo(22L, "DONE", null);
        doneTodo.setDueDate(LocalDateTime.of(2026, 4, 11, 9, 0));
        doneTodo.setRecurrenceType("DAILY");
        doneTodo.setRecurrenceInterval(1);
        doneTodo.setNextTriggerTime(LocalDateTime.of(2026, 4, 11, 9, 0));

        when(todoRepository.findAllByIdIn(List.of(21L, 22L))).thenReturn(List.of(recurringTodo, doneTodo));
        when(todoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<TodoItem> result = todoService.batchComplete(List.of(21L, 22L));

        assertEquals(List.of("DONE", "DONE"), result.stream().map(TodoItem::getStatus).toList());
        assertNotNull(recurringTodo.getCompletedAt());
        verify(todoRepository, org.mockito.Mockito.times(2)).saveAll(anyList());
    }

    /**
     * 重复任务在重新打开后再次完成时，不应重复生成同一条下一实例。
     */
    @Test
    void updateShouldNotGenerateDuplicateRecurringSuccessorAfterReopenAndRecomplete() {
        TodoItem existing = createTodo(31L, "PENDING", null);
        existing.setTitle("Daily sync");
        existing.setPriority(3);
        existing.setDueDate(LocalDateTime.of(2026, 4, 10, 9, 0));
        existing.setRecurrenceType("DAILY");
        existing.setRecurrenceInterval(1);
        existing.setRecurrenceEndTime(LocalDateTime.of(2026, 4, 20, 9, 0));
        existing.setNextTriggerTime(LocalDateTime.of(2026, 4, 10, 9, 0));

        TodoItemRequest doneRequest = buildRecurringUpdateRequest("done");
        TodoItemRequest pendingRequest = buildRecurringUpdateRequest("pending");

        when(todoRepository.findByIdAndDeletedAtIsNull(31L)).thenReturn(Optional.of(existing));
        when(todoRepository.save(any(TodoItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(todoRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<TodoItem> entities = invocation.getArgument(0);
            long generatedId = 131L;
            for (TodoItem entity : entities) {
                if (entity.getId() == null) {
                    entity.setId(generatedId++);
                }
            }
            return entities;
        });
        when(todoRepository.existsByDeletedAtIsNullAndTitleAndStatusAndPriorityAndDueDateAndCategoryAndTagsAndRecurrenceTypeAndRecurrenceIntervalAndRecurrenceEndTimeAndNextTriggerTime(
                eq("Daily sync"),
                eq("PENDING"),
                eq(3),
                eq(LocalDateTime.of(2026, 4, 11, 9, 0)),
                eq(null),
                eq(null),
                eq("DAILY"),
                eq(1),
                eq(LocalDateTime.of(2026, 4, 20, 9, 0)),
                eq(LocalDateTime.of(2026, 4, 11, 9, 0))
        )).thenReturn(false, true);

        TodoItem firstCompletion = todoService.update(31L, doneRequest);
        assertEquals("DONE", firstCompletion.getStatus());

        TodoItem reopened = todoService.update(31L, pendingRequest);
        assertEquals("PENDING", reopened.getStatus());

        TodoItem secondCompletion = todoService.update(31L, doneRequest);
        assertEquals("DONE", secondCompletion.getStatus());
        verify(todoRepository, times(1)).saveAll(anyList());
    }

    /**
     * 重复任务生成下一条实例时，应将 nextTriggerTime 视为当前实例的计划锚点，并把下一实例的 dueDate 与 nextTriggerTime
     * 同步到同一个下一次计划时间。
     */
    @Test
    void updateShouldGenerateRecurringSuccessorWithNextTriggerTimeForNextOccurrence() {
        TodoItem existing = createTodo(32L, "PENDING", null);
        existing.setTitle("Weekly review");
        existing.setPriority(4);
        existing.setDueDate(LocalDateTime.of(2026, 4, 10, 9, 0));
        existing.setRecurrenceType("WEEKLY");
        existing.setRecurrenceInterval(2);
        existing.setRecurrenceEndTime(LocalDateTime.of(2026, 6, 1, 9, 0));
        existing.setNextTriggerTime(LocalDateTime.of(2026, 4, 10, 9, 0));

        TodoItemRequest doneRequest = new TodoItemRequest();
        doneRequest.setTitle("Weekly review");
        doneRequest.setStatus("done");
        doneRequest.setPriority(4);
        doneRequest.setDueDate(LocalDateTime.of(2026, 4, 10, 9, 0));
        doneRequest.setRecurrenceType("weekly");
        doneRequest.setRecurrenceInterval(2);
        doneRequest.setRecurrenceEndTime(LocalDateTime.of(2026, 6, 1, 9, 0));

        when(todoRepository.findByIdAndDeletedAtIsNull(32L)).thenReturn(Optional.of(existing));
        when(todoRepository.save(any(TodoItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(todoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        todoService.update(32L, doneRequest);

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<TodoItem>> generatedTodosCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(todoRepository).saveAll(generatedTodosCaptor.capture());
        TodoItem generatedTodo = generatedTodosCaptor.getValue().get(0);

        assertEquals(LocalDateTime.of(2026, 4, 24, 9, 0), generatedTodo.getDueDate());
        assertEquals(LocalDateTime.of(2026, 4, 24, 9, 0), generatedTodo.getNextTriggerTime());
        assertEquals(LocalDateTime.of(2026, 4, 10, 9, 0), existing.getNextTriggerTime());
    }

    /**
     * 批量删除应写入软删除时间，而不是物理删除。
     */
    @Test
    void batchDeleteShouldSetDeletedAt() {
        TodoItem todo1 = createTodo(1L, "PENDING", null);
        TodoItem todo2 = createTodo(2L, "DONE", null);

        when(todoRepository.findAllByIdIn(List.of(1L, 2L))).thenReturn(List.of(todo1, todo2));
        when(todoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        todoService.batchDelete(List.of(1L, 2L));

        assertEquals(2, List.of(todo1, todo2).stream().filter(todo -> todo.getDeletedAt() != null).count());
    }

    /**
     * 恢复回收站中的任务时应清空 deletedAt 字段。
     */
    @Test
    void restoreShouldClearDeletedAt() {
        TodoItem deletedTodo = createTodo(7L, "DONE", LocalDateTime.now());
        when(todoRepository.findById(7L)).thenReturn(Optional.of(deletedTodo));
        when(todoRepository.save(any(TodoItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        TodoItem result = todoService.restore(7L);

        assertNull(result.getDeletedAt());
    }

    /**
     * 仅活动任务应参与分类与标签候选项聚合。
     */
    @Test
    void getOptionsShouldAggregateDistinctActiveCategoriesAndTags() {
        TodoItem active1 = createTodo(1L, "PENDING", null);
        active1.setCategory("Work");
        active1.setTags("urgent,backend");
        TodoItem active2 = createTodo(2L, "DONE", null);
        active2.setCategory("Personal");
        active2.setTags("home,urgent");

        when(todoRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class)))
                .thenReturn(List.of(active1, active2));

        TodoOptionResponse response = todoService.getOptions();

        assertEquals(List.of("Personal", "Work"), response.getCategories());
        assertEquals(List.of("backend", "home", "urgent"), response.getTags());
    }

    /**
     * 分页查询应按仓储返回的分页结果封装 PageResponse。
     */
    @Test
    void findAllShouldWrapPageResponse() {
        TodoItem todo = createTodo(1L, "PENDING", null);
        List<Object[]> summaryRows = List.<Object[]>of(new Object[]{1L, 2L, 1L});
        when(todoRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(todo), PageRequest.of(0, 10, Sort.by("createTime")), 1));
        when(todoSubItemRepository.summarizeByTodoIds(List.of(1L))).thenReturn(summaryRows);

        var response = todoService.findAll(new com.personal.toolkit.todo.dto.TodoQueryRequest(), 0, 10, Sort.by("createTime"));

        assertEquals(1, response.getContent().size());
        assertEquals(1, response.getTotalElements());
        assertEquals(2, response.getContent().get(0).getSubItemSummary().getTotalCount());
    }

    /**
     * 查询详情时缓存未命中应回退数据库，并回写对象缓存。
     */
    @Test
    void findByIdShouldFallbackToRepositoryAndCacheResult() {
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        TodoItem todo = createTodo(5L, "PENDING", null);
        List<Object[]> summaryRows = List.<Object[]>of(new Object[]{5L, 3L, 2L});

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(TodoCacheKeys.todoItem(5L))).thenReturn(null);
        when(todoRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(todo));
        when(todoSubItemRepository.summarizeByTodoIds(List.of(5L))).thenReturn(summaryRows);

        TodoItem result = todoService.findById(5L);

        assertEquals(5L, result.getId());
        assertEquals(66, result.getSubItemSummary().getProgressPercent());
        verify(valueOperations).set(anyString(), any(TodoItem.class), any());
    }

    /**
     * 更新待办事项时应复用规范化逻辑并刷新缓存。
     */
    @Test
    void updateShouldNormalizeFieldsAndRefreshCache() {
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        TodoItem existing = createTodo(3L, "PENDING", null);
        existing.setCategory("Old");
        existing.setTags("legacy");
        existing.setDueDate(LocalDateTime.of(2026, 4, 10, 9, 0));
        existing.setRecurrenceType("DAILY");
        existing.setRecurrenceInterval(1);
        existing.setNextTriggerTime(LocalDateTime.of(2026, 4, 10, 9, 0));

        TodoItemRequest request = new TodoItemRequest();
        request.setTitle("  Refined title ");
        request.setStatus("done");
        request.setPriority(5);
        request.setCategory("  Work  ");
        request.setDueDate(LocalDateTime.of(2026, 4, 10, 9, 0));
        request.setTags(" api, contract ,api ");
        request.setRecurrenceType("daily");
        request.setRecurrenceInterval(1);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(todoRepository.findByIdAndDeletedAtIsNull(3L)).thenReturn(Optional.of(existing));
        when(todoRepository.save(any(TodoItem.class))).thenAnswer(invocation -> {
            TodoItem entity = invocation.getArgument(0);
            entity.setId(3L);
            return entity;
        });
        when(todoRepository.saveAll(anyList())).thenAnswer(invocation -> {
            List<TodoItem> entities = invocation.getArgument(0);
            long generatedId = 100L;
            for (TodoItem entity : entities) {
                if (entity.getId() == null) {
                    entity.setId(generatedId++);
                }
            }
            return entities;
        });

        TodoItem updated = todoService.update(3L, request);

        assertEquals("Refined title", updated.getTitle());
        assertEquals("DONE", updated.getStatus());
        assertEquals("Work", updated.getCategory());
        assertEquals("api,contract", updated.getTags());
        assertNotNull(updated.getCompletedAt());
        verify(valueOperations, org.mockito.Mockito.times(2)).set(anyString(), any(TodoItem.class), any());
    }

    /**
     * 创建与更新等写操作缓存父任务时应同时写入 checklist 摘要字段。
     */
    @Test
    void createShouldCacheTodoWithHydratedSummary() {
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);

        TodoItemRequest request = new TodoItemRequest();
        request.setTitle("Checklist parent");
        request.setStatus("PENDING");
        request.setPriority(3);

        TodoItem saved = createTodo(11L, "PENDING", null);
        List<Object[]> summaryRows = List.<Object[]>of(new Object[]{11L, 2L, 1L});

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(todoRepository.save(any(TodoItem.class))).thenReturn(saved);
        when(todoSubItemRepository.summarizeByTodoIds(List.of(11L))).thenReturn(summaryRows);

        TodoItem result = todoService.create(request);

        assertEquals(2, result.getSubItemSummary().getTotalCount());
        assertEquals(1, result.getSubItemSummary().getCompletedCount());
        verify(valueOperations).set(anyString(), any(TodoItem.class), any());
    }

    /**
     * 删除待办事项时应执行软删除并清理对象缓存。
     */
    @Test
    void deleteShouldSoftDeleteAndEvictCache() {
        TodoItem existing = createTodo(8L, "PENDING", null);

        when(todoRepository.findByIdAndDeletedAtIsNull(8L)).thenReturn(Optional.of(existing));
        when(todoRepository.save(any(TodoItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        todoService.delete(8L);

        assertEquals(8L, existing.getId());
        assertEquals("PENDING", existing.getStatus());
        assertEquals(true, existing.getDeletedAt() != null);
        verify(redisTemplate).delete(TodoCacheKeys.todoItem(8L));
    }

    /**
     * 子任务写入后再次读取父任务详情时，应因缓存失效而回源并返回最新摘要。
     */
    @Test
    void subItemWriteShouldEvictParentCacheAndRefreshDetailSummaryOnNextRead() {
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        TodoSubItemService todoSubItemService = new TodoSubItemService(todoSubItemRepository, todoRepository, redisTemplate);
        Map<String, Object> cache = new HashMap<>();
        String cacheKey = TodoCacheKeys.todoItem(15L);

        TodoItem staleCachedTodo = createTodo(15L, "PENDING", null);
        staleCachedTodo.setSubItemSummary(new TodoSubItemSummaryResponse(1, 0, 0));
        cache.put(cacheKey, staleCachedTodo);

        TodoItem persistedTodo = createTodo(15L, "PENDING", null);
        TodoSubItemRequest request = new TodoSubItemRequest();
        request.setTitle("Write integration-style test");
        request.setStatus("DONE");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenAnswer(invocation -> cache.get(invocation.getArgument(0)));
        when(todoRepository.existsByIdAndDeletedAtIsNull(15L)).thenReturn(true);
        when(todoRepository.findByIdAndDeletedAtIsNull(15L)).thenReturn(Optional.of(persistedTodo));
        when(todoSubItemRepository.save(any(com.personal.toolkit.todo.entity.TodoSubItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(todoSubItemRepository.summarizeByTodoIds(List.of(15L))).thenReturn(List.<Object[]>of(new Object[]{15L, 2L, 1L}));

        org.mockito.Mockito.doAnswer(invocation -> {
            cache.remove(invocation.getArgument(0));
            return true;
        }).when(redisTemplate).delete(anyString());
        org.mockito.Mockito.doAnswer(invocation -> {
            cache.put(invocation.getArgument(0), invocation.getArgument(1));
            return null;
        }).when(valueOperations).set(anyString(), any(TodoItem.class), any(Duration.class));

        todoSubItemService.create(15L, request);
        TodoItem refreshedTodo = todoService.findById(15L);

        assertEquals(2, refreshedTodo.getSubItemSummary().getTotalCount());
        assertEquals(1, refreshedTodo.getSubItemSummary().getCompletedCount());
        assertEquals(50, refreshedTodo.getSubItemSummary().getProgressPercent());
        assertEquals(2, ((TodoItem) cache.get(cacheKey)).getSubItemSummary().getTotalCount());
        verify(redisTemplate).delete(cacheKey);
    }

    private TodoItem createTodo(Long id, String status, LocalDateTime deletedAt) {
        TodoItem todo = new TodoItem();
        todo.setId(id);
        todo.setTitle("Todo-" + id);
        todo.setStatus(status);
        todo.setPriority(3);
        todo.setDeletedAt(deletedAt);
        return todo;
    }

    private TodoItemRequest buildRecurringUpdateRequest(String status) {
        TodoItemRequest request = new TodoItemRequest();
        request.setTitle("Daily sync");
        request.setStatus(status);
        request.setPriority(3);
        request.setDueDate(LocalDateTime.of(2026, 4, 10, 9, 0));
        request.setRecurrenceType("daily");
        request.setRecurrenceInterval(1);
        request.setRecurrenceEndTime(LocalDateTime.of(2026, 4, 20, 9, 0));
        return request;
    }
}
