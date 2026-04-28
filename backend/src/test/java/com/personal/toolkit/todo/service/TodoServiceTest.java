package com.personal.toolkit.todo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.toolkit.auth.entity.AppUser;
import com.personal.toolkit.auth.repository.AppUserRepository;
import com.personal.toolkit.auth.security.CurrentUserProvider;
import com.personal.toolkit.todo.dto.TodoItemRequest;
import com.personal.toolkit.todo.dto.TodoOptionResponse;
import com.personal.toolkit.todo.dto.TodoQueryRequest;
import com.personal.toolkit.todo.dto.TodoStatsAgingResponse;
import com.personal.toolkit.todo.dto.TodoStatsDueBucketsResponse;
import com.personal.toolkit.todo.dto.TodoStatsOverviewResponse;
import com.personal.toolkit.todo.dto.TodoStatsPriorityDistributionResponse;
import com.personal.toolkit.todo.dto.TodoStatsRecurrenceDistributionResponse;
import com.personal.toolkit.todo.dto.TodoStatsTrendResponse;
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
import java.time.LocalDate;
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
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.lenient;
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

    @Mock
    private TodoReminderService todoReminderService;

    private TodoService todoService;

    @BeforeEach
    void setUp() {
        todoService = new TodoService(
                todoRepository,
                todoSubItemRepository,
                redisTemplate,
                new ObjectMapper(),
                currentUserProvider,
                appUserRepository,
                todoReminderService
        );
        lenient().when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
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
        when(todoReminderService.countUnreadReminders(USER_ID)).thenReturn(4L);

        TodoStatsOverviewResponse response = todoService.getStatsOverview();

        assertEquals(2L, response.getTodayCompleted());
        assertEquals(7L, response.getWeekCompleted());
        assertEquals(3L, response.getOverdueCount());
        assertEquals(11L, response.getActiveCount());
        assertEquals(5L, response.getUpcomingReminderCount());
        assertEquals(4L, response.getUnreadReminderCount());
    }

    /**
     * 截止时间桶统计应按活动任务和固定日期区间聚合。
     */
    @Test
    void getDueBucketsStatsShouldAggregateCurrentUserBuckets() {
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNot(USER_ID, "DONE")).thenReturn(20L);
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateBefore(eq(USER_ID), eq("DONE"), any(LocalDateTime.class))).thenReturn(3L);
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateBetween(eq(USER_ID), eq("DONE"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(4L, 5L, 6L);
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateIsNull(USER_ID, "DONE")).thenReturn(2L);

        TodoStatsDueBucketsResponse response = todoService.getDueBucketsStats();

        assertEquals(3L, response.getOverdue());
        assertEquals(4L, response.getDueToday());
        assertEquals(5L, response.getDueIn3Days());
        assertEquals(6L, response.getDueIn7Days());
        assertEquals(2L, response.getNoDueDate());
        assertEquals(20L, response.getTotalActive());
    }

    /**
     * 截止时间桶查询应使用互不重叠的日期边界，避免相邻桶重复计数。
     */
    @Test
    void getDueBucketsStatsShouldUseNonOverlappingDateBoundaries() {
        LocalDate today = LocalDate.now();
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNot(USER_ID, "DONE")).thenReturn(0L);
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateBefore(eq(USER_ID), eq("DONE"), argThat(boundary -> today.atStartOfDay().equals(boundary))))
                .thenReturn(0L);
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateBetween(
                eq(USER_ID),
                eq("DONE"),
                argThat(start -> today.atStartOfDay().equals(start)),
                argThat(end -> today.atTime(23, 59, 59).equals(end))
        )).thenReturn(0L);
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateBetween(
                eq(USER_ID),
                eq("DONE"),
                argThat(start -> today.plusDays(1).atStartOfDay().equals(start)),
                argThat(end -> today.plusDays(3).atTime(23, 59, 59).equals(end))
        )).thenReturn(0L);
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateBetween(
                eq(USER_ID),
                eq("DONE"),
                argThat(start -> today.plusDays(4).atStartOfDay().equals(start)),
                argThat(end -> today.plusDays(7).atTime(23, 59, 59).equals(end))
        )).thenReturn(0L);
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateIsNull(USER_ID, "DONE")).thenReturn(0L);

        TodoStatsDueBucketsResponse response = todoService.getDueBucketsStats();

        assertEquals(0L, response.getOverdue());
        assertEquals(0L, response.getDueToday());
        assertEquals(0L, response.getDueIn3Days());
        assertEquals(0L, response.getDueIn7Days());
        assertEquals(0L, response.getNoDueDate());
        verify(todoRepository).countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateBefore(eq(USER_ID), eq("DONE"), argThat(boundary -> today.atStartOfDay().equals(boundary)));
    }

    /**
     * 优先级分布统计应返回仓储聚合结果和活动总数。
     */
    @Test
    void getPriorityDistributionStatsShouldReturnPriorityItems() {
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNot(USER_ID, "DONE")).thenReturn(9L);
        when(todoRepository.summarizeActiveByPriority(USER_ID)).thenReturn(List.of(
                new Object[]{1, 2L},
                new Object[]{3, 7L}
        ));

        TodoStatsPriorityDistributionResponse response = todoService.getPriorityDistributionStats();

        assertEquals(9L, response.getTotalActive());
        assertEquals(2, response.getItems().size());
        assertEquals(1, response.getItems().get(0).getPriority());
        assertEquals(2L, response.getItems().get(0).getCount());
        assertEquals(3, response.getItems().get(1).getPriority());
        assertEquals(7L, response.getItems().get(1).getCount());
    }

    /**
     * 老化统计应按固定创建时长桶聚合当前用户的活动任务。
     */
    @Test
    void getAgingStatsShouldReturnFixedBuckets() {
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNot(USER_ID, "DONE")).thenReturn(24L);
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndCreateTimeBetween(eq(USER_ID), eq("DONE"), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(8L, 5L, 6L, 3L);
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndCreateTimeBefore(eq(USER_ID), eq("DONE"), any(LocalDateTime.class)))
                .thenReturn(2L);

        TodoStatsAgingResponse response = todoService.getAgingStats();

        assertEquals(24L, response.getTotalPending());
        assertEquals(5, response.getBuckets().size());
        assertEquals("0_3_DAYS", response.getBuckets().get(0).getLabel());
        assertEquals(8L, response.getBuckets().get(0).getCount());
        assertEquals("OVER_30_DAYS", response.getBuckets().get(4).getLabel());
        assertEquals(2L, response.getBuckets().get(4).getCount());
        assertEquals(
                response.getTotalPending(),
                response.getBuckets().stream().mapToLong(bucket -> bucket.getCount()).sum()
        );
    }

    /**
     * 重复类型分布应返回稳定顺序，并为缺失桶补零。
     */
    @Test
    void getRecurrenceDistributionStatsShouldReturnStableBuckets() {
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNot(USER_ID, "DONE")).thenReturn(10L);
        when(todoRepository.summarizeActiveByRecurrenceType(USER_ID)).thenReturn(List.of(
                new Object[]{"DAILY", 3L},
                new Object[]{"MONTHLY", 2L}
        ));

        TodoStatsRecurrenceDistributionResponse response = todoService.getRecurrenceDistributionStats();

        assertEquals(10L, response.getTotalActive());
        assertEquals(List.of("NONE", "DAILY", "WEEKLY", "MONTHLY"), response.getItems().stream().map(item -> item.getRecurrenceType()).toList());
        assertEquals(0L, response.getItems().get(0).getCount());
        assertEquals(3L, response.getItems().get(1).getCount());
        assertEquals(0L, response.getItems().get(2).getCount());
        assertEquals(2L, response.getItems().get(3).getCount());
    }

    /**
     * 空白重复类型也应归并到 NONE，避免分子分母不一致。
     */
    @Test
    void getRecurrenceDistributionStatsShouldFoldBlankTypeIntoNoneBucket() {
        when(todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNot(USER_ID, "DONE")).thenReturn(10L);
        when(todoRepository.summarizeActiveByRecurrenceType(USER_ID)).thenReturn(List.of(
                new Object[]{" ", 4L},
                new Object[]{"DAILY", 1L}
        ));

        TodoStatsRecurrenceDistributionResponse response = todoService.getRecurrenceDistributionStats();

        assertEquals(10L, response.getTotalActive());
        assertEquals(List.of("NONE", "DAILY", "WEEKLY", "MONTHLY"), response.getItems().stream().map(item -> item.getRecurrenceType()).toList());
        assertEquals(4L, response.getItems().get(0).getCount());
        assertEquals(1L, response.getItems().get(1).getCount());
        assertEquals(0L, response.getItems().get(2).getCount());
        assertEquals(0L, response.getItems().get(3).getCount());
    }

    /**
     * 趋势统计应同时返回最近 7 天的新建与完成序列，并补齐汇总字段。
     */
    @Test
    void getStatsTrendShouldReturnCreatedCompletedSeriesAndSummary() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);
        when(todoRepository.findCreatedAtBetween(USER_ID, start, end)).thenReturn(List.of(
                startDate.atTime(9, 0),
                startDate.atTime(10, 0),
                startDate.plusDays(2).atTime(11, 0)
        ));
        when(todoRepository.findCompletedAtBetween(USER_ID, start, end)).thenReturn(List.of(
                startDate.atTime(12, 0),
                startDate.plusDays(1).atTime(13, 0)
        ));

        TodoStatsTrendResponse response = todoService.getStatsTrend("7d");

        assertEquals("7d", response.getRange());
        assertEquals(7, response.getItems().size());
        assertEquals(startDate.toString(), response.getItems().get(0).getDate());
        assertEquals(2L, response.getItems().get(0).getCreatedCount());
        assertEquals(1L, response.getItems().get(0).getCompletedCount());
        assertEquals(startDate.plusDays(1).toString(), response.getItems().get(1).getDate());
        assertEquals(0L, response.getItems().get(1).getCreatedCount());
        assertEquals(1L, response.getItems().get(1).getCompletedCount());
        assertEquals(startDate.plusDays(2).toString(), response.getItems().get(2).getDate());
        assertEquals(1L, response.getItems().get(2).getCreatedCount());
        assertEquals(0L, response.getItems().get(2).getCompletedCount());
        assertEquals(3L, response.getSummary().getTotalCreated());
        assertEquals(2L, response.getSummary().getTotalCompleted());
        assertEquals("0.6667", response.getSummary().getCompletionRate().toPlainString());
        assertEquals(1L, response.getSummary().getNetChange());
    }

    /**
     * 趋势统计应支持最近 30 天范围，并按请求范围补齐日期。
     */
    @Test
    void getStatsTrendShouldSupport30dRange() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(29);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);
        when(todoRepository.findCreatedAtBetween(USER_ID, start, end)).thenReturn(List.of(
                startDate.atTime(9, 0),
                today.atTime(10, 0)
        ));
        when(todoRepository.findCompletedAtBetween(USER_ID, start, end)).thenReturn(List.of(
                startDate.plusDays(1).atTime(12, 0),
                today.atTime(13, 0)
        ));

        TodoStatsTrendResponse response = todoService.getStatsTrend("30d");

        assertEquals("30d", response.getRange());
        assertEquals(30, response.getItems().size());
        assertEquals(startDate.toString(), response.getItems().get(0).getDate());
        assertEquals(today.toString(), response.getItems().get(29).getDate());
        assertEquals(2L, response.getSummary().getTotalCreated());
        assertEquals(2L, response.getSummary().getTotalCompleted());
    }

    /**
     * 趋势统计应支持最近 90 天范围，并保留同样的汇总结构。
     */
    @Test
    void getStatsTrendShouldSupport90dRange() {
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(89);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);
        when(todoRepository.findCreatedAtBetween(USER_ID, start, end)).thenReturn(List.of(
                startDate.atTime(9, 0)
        ));
        when(todoRepository.findCompletedAtBetween(USER_ID, start, end)).thenReturn(List.of(
                today.atTime(13, 0)
        ));

        TodoStatsTrendResponse response = todoService.getStatsTrend("90d");

        assertEquals("90d", response.getRange());
        assertEquals(90, response.getItems().size());
        assertEquals(startDate.toString(), response.getItems().get(0).getDate());
        assertEquals(today.toString(), response.getItems().get(89).getDate());
        assertEquals(1L, response.getSummary().getTotalCreated());
        assertEquals(1L, response.getSummary().getTotalCompleted());
        assertEquals("1", response.getSummary().getCompletionRate().toPlainString());
    }

    /**
     * 非法趋势范围应继续返回 400，避免不支持的时间范围进入统计逻辑。
     */
    @Test
    void getStatsTrendShouldRejectUnsupportedRange() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> todoService.getStatsTrend("14d"));

        assertEquals(400, exception.getStatusCode().value());
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
