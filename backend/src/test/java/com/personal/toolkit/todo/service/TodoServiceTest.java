package com.personal.toolkit.todo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.toolkit.auth.entity.AppUser;
import com.personal.toolkit.auth.repository.AppUserRepository;
import com.personal.toolkit.auth.security.CurrentUserProvider;
import com.personal.toolkit.todo.dto.TodoItemRequest;
import com.personal.toolkit.todo.dto.TodoOptionResponse;
import com.personal.toolkit.todo.dto.TodoQueryRequest;
import com.personal.toolkit.todo.dto.TodoStatsOverviewResponse;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 TodoService 在引入用户隔离后的缓存键、归属校验与核心业务行为。
 */
@ExtendWith(MockitoExtension.class)
class TodoServiceTest {

    private static final Long USER_ID = 101L;

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private TodoSubItemRepository todoSubItemRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    @Mock
    private AppUserRepository appUserRepository;

    private TodoService todoService;

    @BeforeEach
    void setUp() {
        todoService = new TodoService(
                todoRepository,
                todoSubItemRepository,
                redisTemplate,
                new ObjectMapper(),
                currentUserProvider,
                appUserRepository
        );
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
    }

    /**
     * 缓存命中详情时也应使用带 userId 的缓存键补齐摘要并刷新缓存。
     */
    @Test
    void findByIdShouldHydrateCachedTodoSummaryAndRefreshCache() {
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        TodoItem cachedTodo = createTodo(9L, "PENDING", null);
        cachedTodo.setSubItemSummary(null);
        List<Object[]> summaryRows = List.<Object[]>of(new Object[]{9L, 4L, 1L});

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(TodoCacheKeys.todoItem(USER_ID, 9L))).thenReturn(cachedTodo);
        when(todoSubItemRepository.summarizeByTodoIds(List.of(9L))).thenReturn(summaryRows);

        TodoItem result = todoService.findById(9L);

        assertEquals(4, result.getSubItemSummary().getTotalCount());
        assertEquals(1, result.getSubItemSummary().getCompletedCount());
        assertEquals(25, result.getSubItemSummary().getProgressPercent());
        verify(valueOperations).set(eq(TodoCacheKeys.todoItem(USER_ID, 9L)), any(TodoItem.class), any());
        verify(todoRepository, never()).findByIdAndUserIdAndDeletedAtIsNull(9L, USER_ID);
    }

    /**
     * 创建待办事项时应写入当前用户归属并使用带 userId 的缓存键。
     */
    @Test
    void createShouldAssignCurrentUserAndUseUserScopedCacheKey() {
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(createUser()));

        TodoItemRequest request = new TodoItemRequest();
        request.setTitle("  Write tests  ");
        request.setStatus("pending");
        request.setPriority(4);
        request.setRemindAt(LocalDateTime.of(2026, 4, 11, 8, 30));
        request.setCategory("  Work  ");
        request.setTags(" urgent, backend ,urgent ");
        request.setNotes("  Write backend summary  ");
        request.setAttachmentLinks(" https://example.com/spec \nhttps://example.com/spec\nhttps://example.com/runbook ");
        request.setOwnerLabel(" Alice ");
        request.setCollaborators(" Bob, Carol ,Bob ");
        request.setWatchers(" Dave, Erin ");

        when(todoRepository.save(any(TodoItem.class))).thenAnswer(invocation -> {
            TodoItem entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        TodoItem result = todoService.create(request);

        assertEquals(USER_ID, result.getUser().getId());
        assertEquals("Write tests", result.getTitle());
        assertEquals("Work", result.getCategory());
        assertEquals("urgent,backend", result.getTags());
        assertEquals(LocalDateTime.of(2026, 4, 11, 8, 30), result.getRemindAt());
        assertEquals("Write backend summary", result.getNotes());
        assertEquals("https://example.com/spec\nhttps://example.com/runbook", result.getAttachmentLinks());
        assertEquals("Alice", result.getOwnerLabel());
        assertEquals("Bob,Carol", result.getCollaborators());
        assertEquals("Dave,Erin", result.getWatchers());
        verify(valueOperations).set(eq(TodoCacheKeys.todoItem(USER_ID, 1L)), any(TodoItem.class), any());
    }

    /**
     * 非法优先级应被拒绝，避免脏数据写入数据库。
     */
    @Test
    void createShouldRejectInvalidPriority() {
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(createUser()));

        TodoItemRequest request = new TodoItemRequest();
        request.setTitle("Bad priority");
        request.setStatus("PENDING");
        request.setPriority(8);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> todoService.create(request));

        assertEquals(400, exception.getStatusCode().value());
        verify(todoRepository, never()).save(any(TodoItem.class));
    }

    /**
     * 跨用户读取详情时应表现为 404，避免暴露其他用户数据是否存在。
     */
    @Test
    void findByIdShouldReturnNotFoundForOtherUsersTodo() {
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(TodoCacheKeys.todoItem(USER_ID, 55L))).thenReturn(null);
        when(todoRepository.findByIdAndUserIdAndDeletedAtIsNull(55L, USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> todoService.findById(55L));

        assertEquals(404, exception.getStatusCode().value());
        assertEquals("Todo item not found: 55", exception.getReason());
    }

    /**
     * 批量完成重复任务时生成的下一条实例应继承同一归属用户，避免跨用户串单。
     */
    @Test
    void batchCompleteShouldGenerateRecurringTodoForSameUser() {
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        TodoItem recurringTodo = createTodo(21L, "PENDING", null);
        recurringTodo.setTitle("Daily sync");
        recurringTodo.setDueDate(LocalDateTime.of(2026, 4, 10, 9, 0));
        recurringTodo.setRemindAt(LocalDateTime.of(2026, 4, 10, 8, 30));
        recurringTodo.setNotes("Run sync agenda");
        recurringTodo.setAttachmentLinks("https://example.com/sync");
        recurringTodo.setOwnerLabel("Alice");
        recurringTodo.setCollaborators("Bob,Carol");
        recurringTodo.setWatchers("Dave");
        recurringTodo.setRecurrenceType("DAILY");
        recurringTodo.setRecurrenceInterval(1);
        recurringTodo.setNextTriggerTime(LocalDateTime.of(2026, 4, 10, 9, 0));

        when(todoRepository.findAllByUserIdAndIdIn(USER_ID, List.of(21L))).thenReturn(List.of(recurringTodo));
        when(todoRepository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));
        when(todoRepository.existsByUserIdAndDeletedAtIsNullAndTitleAndStatusAndPriorityAndDueDateAndRemindAtAndCategoryAndTagsAndRecurrenceTypeAndRecurrenceIntervalAndRecurrenceEndTimeAndNextTriggerTime(
                eq(USER_ID),
                eq("Daily sync"),
                eq("PENDING"),
                eq(3),
                eq(LocalDateTime.of(2026, 4, 11, 9, 0)),
                eq(LocalDateTime.of(2026, 4, 11, 8, 30)),
                eq(null),
                eq(null),
                eq("DAILY"),
                eq(1),
                eq(null),
                eq(LocalDateTime.of(2026, 4, 11, 9, 0))
        )).thenReturn(false);

        todoService.batchComplete(List.of(21L));

        @SuppressWarnings("unchecked")
        org.mockito.ArgumentCaptor<List<TodoItem>> generatedTodosCaptor = org.mockito.ArgumentCaptor.forClass(List.class);
        verify(todoRepository, times(2)).saveAll(generatedTodosCaptor.capture());
        TodoItem generatedTodo = generatedTodosCaptor.getAllValues().get(1).get(0);
        assertEquals(USER_ID, generatedTodo.getUser().getId());
        assertEquals(LocalDateTime.of(2026, 4, 11, 9, 0), generatedTodo.getDueDate());
        assertEquals(LocalDateTime.of(2026, 4, 11, 8, 30), generatedTodo.getRemindAt());
        assertEquals("Run sync agenda", generatedTodo.getNotes());
        assertEquals("https://example.com/sync", generatedTodo.getAttachmentLinks());
        assertEquals("Alice", generatedTodo.getOwnerLabel());
        assertEquals("Bob,Carol", generatedTodo.getCollaborators());
        assertEquals("Dave", generatedTodo.getWatchers());
    }

    /**
     * 统计概览应按当前用户范围聚合，避免不同用户任务数相互污染。
     */
    @Test
    void getStatsOverviewShouldUseCurrentUserScope() {
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndCompletedAtBetween(eq(USER_ID), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(2L, 7L);
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateBefore(eq(USER_ID), eq("DONE"), any(LocalDateTime.class))).thenReturn(3L);
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNot(USER_ID, "DONE")).thenReturn(11L);
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndRemindAtBetween(eq(USER_ID), eq("DONE"), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(5L);

        TodoStatsOverviewResponse response = todoService.getStatsOverview();

        assertEquals(2L, response.getTodayCompleted());
        assertEquals(7L, response.getWeekCompleted());
        assertEquals(3L, response.getOverdueCount());
        assertEquals(11L, response.getActiveCount());
        assertEquals(5L, response.getUpcomingReminderCount());
    }

    /**
     * 候选项聚合应基于当前用户的活动任务集合构建分类和标签列表。
     */
    @Test
    void getOptionsShouldAggregateDistinctUserScopedCategoriesAndTags() {
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
     * 分页查询应按仓储返回的用户范围分页结果封装 PageResponse。
     */
    @Test
    void findAllShouldWrapPageResponse() {
        TodoItem todo = createTodo(1L, "PENDING", null);
        List<Object[]> summaryRows = List.<Object[]>of(new Object[]{1L, 2L, 1L});
        when(todoRepository.findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(todo), PageRequest.of(0, 10, Sort.by("createTime")), 1));
        when(todoSubItemRepository.summarizeByTodoIds(List.of(1L))).thenReturn(summaryRows);

        var response = todoService.findAll(new TodoQueryRequest(), 0, 10, Sort.by("createTime"));

        assertEquals(1, response.getContent().size());
        assertEquals(2, response.getContent().get(0).getSubItemSummary().getTotalCount());
    }

    /**
     * 无效的重复筛选值应在进入仓储层前被拒绝。
     */
    @Test
    void findAllShouldRejectInvalidRecurrenceFilter() {
        TodoQueryRequest queryRequest = new TodoQueryRequest();
        queryRequest.setRecurrenceType("YEARLY");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.findAll(queryRequest, 0, 10, Sort.by("createTime"))
        );

        assertEquals(400, exception.getStatusCode().value());
        verify(todoRepository, never()).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class));
    }

    /**
     * 无效的时间预设值应在进入仓储层前被拒绝。
     */
    @Test
    void findAllShouldRejectInvalidTimePreset() {
        TodoQueryRequest queryRequest = new TodoQueryRequest();
        queryRequest.setTimePreset("NEXT_MONTH");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> todoService.findAll(queryRequest, 0, 10, Sort.by("createTime"))
        );

        assertEquals(400, exception.getStatusCode().value());
        verify(todoRepository, never()).findAll(any(org.springframework.data.jpa.domain.Specification.class), any(PageRequest.class));
    }

    /**
     * 删除待办事项时应执行软删除并清理带 userId 的对象缓存。
     */
    @Test
    void deleteShouldSoftDeleteAndEvictUserScopedCache() {
        TodoItem existing = createTodo(8L, "PENDING", null);
        when(todoRepository.findByIdAndUserIdAndDeletedAtIsNull(8L, USER_ID)).thenReturn(Optional.of(existing));
        when(todoRepository.save(any(TodoItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        todoService.delete(8L);

        assertNotNull(existing.getDeletedAt());
        verify(redisTemplate).delete(TodoCacheKeys.todoItem(USER_ID, 8L));
    }

    /**
     * checklist 写入后再次读取父任务详情时，应通过用户维度缓存键刷新最新摘要。
     */
    @Test
    void subItemWriteShouldEvictParentCacheAndRefreshDetailSummaryOnNextRead() {
        ValueOperations<String, Object> valueOperations = mock(ValueOperations.class);
        TodoSubItemService todoSubItemService = new TodoSubItemService(todoSubItemRepository, todoRepository, redisTemplate, currentUserProvider);
        Map<String, Object> cache = new HashMap<>();
        String cacheKey = TodoCacheKeys.todoItem(USER_ID, 15L);

        TodoItem staleCachedTodo = createTodo(15L, "PENDING", null);
        staleCachedTodo.setSubItemSummary(new TodoSubItemSummaryResponse(1, 0, 0));
        cache.put(cacheKey, staleCachedTodo);

        TodoItem persistedTodo = createTodo(15L, "PENDING", null);
        TodoSubItemRequest request = new TodoSubItemRequest();
        request.setTitle("Write integration-style test");
        request.setStatus("DONE");

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenAnswer(invocation -> cache.get(invocation.getArgument(0)));
        when(todoRepository.existsByIdAndUserIdAndDeletedAtIsNull(15L, USER_ID)).thenReturn(true);
        when(todoRepository.findByIdAndUserIdAndDeletedAtIsNull(15L, USER_ID)).thenReturn(Optional.of(persistedTodo));
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
        verify(redisTemplate).delete(cacheKey);
    }

    private AppUser createUser() {
        AppUser appUser = new AppUser();
        appUser.setId(USER_ID);
        appUser.setUsername("alice");
        appUser.setEmail("alice@example.com");
        appUser.setPasswordHash("encoded");
        return appUser;
    }

    private TodoItem createTodo(Long id, String status, LocalDateTime deletedAt) {
        TodoItem todo = new TodoItem();
        todo.setId(id);
        todo.setUser(createUser());
        todo.setTitle("Todo-" + id);
        todo.setStatus(status);
        todo.setPriority(3);
        todo.setDeletedAt(deletedAt);
        return todo;
    }
}
