package com.personal.toolkit.todo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.toolkit.auth.entity.AppUser;
import com.personal.toolkit.auth.security.CurrentUserProvider;
import com.personal.toolkit.todo.dto.TodoQueryRequest;
import com.personal.toolkit.todo.dto.TodoItemRequest;
import com.personal.toolkit.todo.dto.PageResponse;
import com.personal.toolkit.todo.dto.TodoOptionResponse;
import com.personal.toolkit.todo.dto.TodoStatsCategoryItemResponse;
import com.personal.toolkit.todo.dto.TodoStatsDueBucketsResponse;
import com.personal.toolkit.todo.dto.TodoStatsOverviewResponse;
import com.personal.toolkit.todo.dto.TodoStatsPriorityDistributionItemResponse;
import com.personal.toolkit.todo.dto.TodoStatsPriorityDistributionResponse;
import com.personal.toolkit.todo.dto.TodoStatsTrendItemResponse;
import com.personal.toolkit.todo.dto.TodoStatsTrendResponse;
import com.personal.toolkit.todo.dto.TodoStatsTrendSummaryResponse;
import com.personal.toolkit.todo.dto.TodoSubItemSummaryResponse;
import com.personal.toolkit.todo.entity.TodoItem;
import com.personal.toolkit.auth.repository.AppUserRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.DayOfWeek;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.TemporalAdjusters;
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
    private static final Set<String> ALLOWED_TIME_PRESETS = Set.of("DUE_TODAY", "OVERDUE", "UPCOMING_REMINDER");
    private static final String SUPPORTED_TREND_RANGE = "7d";
    private static final String UNCLASSIFIED_CATEGORY_KEY = "__UNCLASSIFIED__";

    private final TodoRepository todoRepository;
    private final TodoSubItemRepository todoSubItemRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;
    private final CurrentUserProvider currentUserProvider;
    private final AppUserRepository appUserRepository;
    private final TodoReminderService todoReminderService;

    public TodoService(TodoRepository todoRepository,
                       TodoSubItemRepository todoSubItemRepository,
                       RedisTemplate<String, Object> redisTemplate,
                       ObjectMapper objectMapper,
                       CurrentUserProvider currentUserProvider,
                       AppUserRepository appUserRepository,
                       TodoReminderService todoReminderService) {
        this.todoRepository = todoRepository;
        this.todoSubItemRepository = todoSubItemRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.currentUserProvider = currentUserProvider;
        this.appUserRepository = appUserRepository;
        this.todoReminderService = todoReminderService;
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
        Page<TodoItem> todoPage = todoRepository.findAll(buildSpecification(queryRequest, currentUserProvider.getCurrentUserId()), pageable);
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
        List<TodoItem> todoItems = todoRepository.findAll(buildSpecification(defaultActiveQuery(), currentUserProvider.getCurrentUserId()));

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
     * 统计概览卡片需要的今日完成、本周完成、逾期与活动任务数量。
     *
     * @return 统计概览数据
     */
    @Transactional(readOnly = true)
    public TodoStatsOverviewResponse getStatsOverview() {
        Long userId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        LocalDate today = now.toLocalDate();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(23, 59, 59);
        LocalDate startOfWeek = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDateTime startOfWeekTime = startOfWeek.atStartOfDay();
        LocalDateTime reminderWindowEnd = now.plusHours(24);

        long todayCompleted = todoRepository.countByUserIdAndDeletedAtIsNullAndCompletedAtBetween(userId, startOfToday, endOfToday);
        long weekCompleted = todoRepository.countByUserIdAndDeletedAtIsNullAndCompletedAtBetween(userId, startOfWeekTime, endOfToday);
        long overdueCount = todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateBefore(userId, "DONE", now);
        long activeCount = todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNot(userId, "DONE");
        long upcomingReminderCount = todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndRemindAtBetween(userId, "DONE", now, reminderWindowEnd);
        long unreadReminderCount = todoReminderService.countUnreadReminders(userId);

        return new TodoStatsOverviewResponse(todayCompleted, weekCompleted, overdueCount, activeCount, upcomingReminderCount, unreadReminderCount);
    }

    /**
     * 聚合分类维度的活动任务数与完成任务数，并将空分类归并为统一占位键。
     *
     * @return 分类统计结果列表
     */
    @Transactional(readOnly = true)
    public List<TodoStatsCategoryItemResponse> getCategoryStats() {
        Map<String, long[]> categoryBuckets = new HashMap<>();
        List<Object[]> rows = todoRepository.summarizeByCategory(currentUserProvider.getCurrentUserId());
        if (rows != null) {
            rows.forEach(row -> {
                String rawCategory = (String) row[0];
                long activeCount = row[1] == null ? 0L : ((Number) row[1]).longValue();
                long completedCount = row[2] == null ? 0L : ((Number) row[2]).longValue();
                String normalizedCategory = normalizeStatsCategory(rawCategory);
                long[] bucket = categoryBuckets.computeIfAbsent(normalizedCategory, key -> new long[]{0L, 0L});
                bucket[0] += activeCount;
                bucket[1] += completedCount;
            });
        }

        return categoryBuckets.entrySet().stream()
                .sorted(Map.Entry.comparingByKey(String::compareToIgnoreCase))
                .map(entry -> new TodoStatsCategoryItemResponse(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
    }

    /**
     * 聚合活动任务在固定截止时间桶中的分布情况，供统计页评估到期压力。
     *
     * @return 截止时间桶统计结果
     */
    @Transactional(readOnly = true)
    public TodoStatsDueBucketsResponse getDueBucketsStats() {
        Long userId = currentUserProvider.getCurrentUserId();
        LocalDate today = LocalDate.now();
        LocalDateTime startOfToday = today.atStartOfDay();
        LocalDateTime endOfToday = today.atTime(23, 59, 59);
        LocalDateTime startOfTomorrow = today.plusDays(1).atStartOfDay();
        LocalDateTime endOfThirdDay = today.plusDays(3).atTime(23, 59, 59);
        LocalDateTime startOfFourthDay = today.plusDays(4).atStartOfDay();
        LocalDateTime endOfSeventhDay = today.plusDays(7).atTime(23, 59, 59);

        long totalActive = todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNot(userId, "DONE");
        long overdue = todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateBefore(userId, "DONE", startOfToday);
        long dueToday = todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateBetween(userId, "DONE", startOfToday, endOfToday);
        long dueIn3Days = todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateBetween(userId, "DONE", startOfTomorrow, endOfThirdDay);
        long dueIn7Days = todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateBetween(userId, "DONE", startOfFourthDay, endOfSeventhDay);
        long noDueDate = todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateIsNull(userId, "DONE");

        return new TodoStatsDueBucketsResponse(overdue, dueToday, dueIn3Days, dueIn7Days, noDueDate, totalActive);
    }

    /**
     * 聚合活动任务在各优先级下的分布，供统计页展示优先级结构。
     *
     * @return 优先级分布统计结果
     */
    @Transactional(readOnly = true)
    public TodoStatsPriorityDistributionResponse getPriorityDistributionStats() {
        Long userId = currentUserProvider.getCurrentUserId();
        long totalActive = todoRepository.countByUserIdAndDeletedAtIsNullAndStatusNot(userId, "DONE");

        List<TodoStatsPriorityDistributionItemResponse> items = todoRepository.summarizeActiveByPriority(userId).stream()
                .map(row -> new TodoStatsPriorityDistributionItemResponse(
                        row[0] == null ? null : ((Number) row[0]).intValue(),
                        row[1] == null ? 0L : ((Number) row[1]).longValue()
                ))
                .toList();

        return new TodoStatsPriorityDistributionResponse(items, totalActive);
    }

    /**
     * 构建最近 7 天完成趋势，并为无数据日期补零，供前端趋势面板直接消费。
     *
     * @param range 时间范围，目前仅支持 7d
     * @return 最近 7 天趋势数据
     */
    @Transactional(readOnly = true)
    public TodoStatsTrendResponse getStatsTrend(String range) {
        if (!SUPPORTED_TREND_RANGE.equalsIgnoreCase(range)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "range must be 7d");
        }

        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(6);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = today.atTime(23, 59, 59);
        Map<LocalDate, Long> createdByDate = new HashMap<>();
        Map<LocalDate, Long> completedByDate = new HashMap<>();

        todoRepository.findCreatedAtBetween(currentUserProvider.getCurrentUserId(), start, end).forEach(createdAt -> {
            LocalDate createdDate = createdAt.toLocalDate();
            createdByDate.put(createdDate, createdByDate.getOrDefault(createdDate, 0L) + 1L);
        });

        todoRepository.findCompletedAtBetween(currentUserProvider.getCurrentUserId(), start, end).forEach(completedAt -> {
            LocalDate completedDate = completedAt.toLocalDate();
            completedByDate.put(completedDate, completedByDate.getOrDefault(completedDate, 0L) + 1L);
        });

        List<TodoStatsTrendItemResponse> items = new ArrayList<>();
        long totalCreated = 0L;
        long totalCompleted = 0L;
        for (int i = 0; i < 7; i++) {
            LocalDate currentDate = startDate.plusDays(i);
            long createdCount = createdByDate.getOrDefault(currentDate, 0L);
            long completedCount = completedByDate.getOrDefault(currentDate, 0L);
            totalCreated += createdCount;
            totalCompleted += completedCount;
            items.add(new TodoStatsTrendItemResponse(currentDate.toString(), createdCount, completedCount));
        }

        return new TodoStatsTrendResponse(
                SUPPORTED_TREND_RANGE,
                items,
                buildTrendSummary(totalCreated, totalCompleted)
        );
    }

    private TodoStatsTrendSummaryResponse buildTrendSummary(long totalCreated, long totalCompleted) {
        BigDecimal completionRate = totalCreated == 0L
                ? BigDecimal.ZERO
                : BigDecimal.valueOf(totalCompleted)
                .divide(BigDecimal.valueOf(totalCreated), 4, RoundingMode.HALF_UP)
                .stripTrailingZeros();

        return new TodoStatsTrendSummaryResponse(
                totalCreated,
                totalCompleted,
                completionRate,
                totalCreated - totalCompleted
        );
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
        todoItem.setUser(getCurrentUserEntity());
        applyRequest(todoItem, request);
        if ("DONE".equals(todoItem.getStatus())) {
            todoItem.setCompletedAt(LocalDateTime.now());
        }

        TodoItem savedTodoItem = todoRepository.save(todoItem);
        todoReminderService.syncReminderForTodo(savedTodoItem);
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
        todoReminderService.syncReminderForTodo(savedTodoItem);
        generatedRecurringTodos.forEach(todoReminderService::syncReminderForTodo);
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
        savedItems.forEach(todoReminderService::syncReminderForTodo);
        generatedRecurringTodos.forEach(todoReminderService::syncReminderForTodo);
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
        todoItems.forEach(todoItem -> todoReminderService.cancelPendingRemindersForTodo(todoItem.getId()));
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
        todoReminderService.syncReminderForTodo(savedTodoItem);
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
        savedItems.forEach(todoReminderService::syncReminderForTodo);
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
        todoReminderService.cancelPendingRemindersForTodo(id);
        runAfterCommit(() -> evictTodoItemCacheSafely(id));
    }

    /**
     * 查询数据库中的待办事项，不存在时抛出 404 异常。
     *
     * @param id 待办事项主键
     * @return 已存在的待办事项实体
     */
    private TodoItem getExistingTodoItem(Long id) {
        return todoRepository.findByIdAndUserIdAndDeletedAtIsNull(id, currentUserProvider.getCurrentUserId())
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
        Long userId = currentUserProvider.getCurrentUserId();
        return (includeDeleted ? todoRepository.findByIdAndUserId(id, userId) : todoRepository.findByIdAndUserIdAndDeletedAtIsNull(id, userId))
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

        List<TodoItem> todoItems = todoRepository.findAllByUserIdAndIdIn(currentUserProvider.getCurrentUserId(), ids).stream()
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

        if (request.getRemindAt() != null && request.getDueDate() != null && request.getRemindAt().isAfter(request.getDueDate())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "remindAt must be before or equal to dueDate");
        }

        todoItem.setTitle(normalizedTitle);
        todoItem.setStatus(normalizedStatus);
        todoItem.setPriority(request.getPriority());
        todoItem.setDueDate(request.getDueDate());
        todoItem.setRemindAt(request.getRemindAt());
        todoItem.setCategory(normalizeNullableText(request.getCategory()));
        todoItem.setTags(normalizeTags(request.getTags()));
        todoItem.setNotes(normalizeNullableText(request.getNotes()));
        todoItem.setAttachmentLinks(normalizeAttachmentLinks(request.getAttachmentLinks()));
        todoItem.setOwnerLabel(normalizeNullableText(request.getOwnerLabel()));
        todoItem.setCollaborators(normalizePeopleList(request.getCollaborators()));
        todoItem.setWatchers(normalizePeopleList(request.getWatchers()));
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
        nextTodo.setUser(completedTodo.getUser());
        nextTodo.setTitle(completedTodo.getTitle());
        nextTodo.setStatus("PENDING");
        nextTodo.setPriority(completedTodo.getPriority());
        nextTodo.setDueDate(nextTriggerTime);
        nextTodo.setRemindAt(calculateNextReminderTime(completedTodo, nextTriggerTime));
        nextTodo.setCategory(completedTodo.getCategory());
        nextTodo.setTags(completedTodo.getTags());
        nextTodo.setNotes(completedTodo.getNotes());
        nextTodo.setAttachmentLinks(completedTodo.getAttachmentLinks());
        nextTodo.setOwnerLabel(completedTodo.getOwnerLabel());
        nextTodo.setCollaborators(completedTodo.getCollaborators());
        nextTodo.setWatchers(completedTodo.getWatchers());
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

        return todoRepository.existsByUserIdAndDeletedAtIsNullAndTitleAndStatusAndPriorityAndDueDateAndRemindAtAndCategoryAndTagsAndRecurrenceTypeAndRecurrenceIntervalAndRecurrenceEndTimeAndNextTriggerTime(
                currentUserProvider.getCurrentUserId(),
                nextTodo.getTitle(),
                nextTodo.getStatus(),
                nextTodo.getPriority(),
                nextTodo.getDueDate(),
                nextTodo.getRemindAt(),
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
     * 让重复任务的下一条实例沿用相对当前截止时间的提醒偏移。
     *
     * @param completedTodo 已完成的重复任务
     * @param nextTriggerTime 下一条实例的计划触发时间
     * @return 下一条实例的提醒时间；若原任务未配置提醒则返回 null
     */
    private LocalDateTime calculateNextReminderTime(TodoItem completedTodo, LocalDateTime nextTriggerTime) {
        if (completedTodo.getRemindAt() == null || nextTriggerTime == null) {
            return null;
        }

        LocalDateTime anchor = resolveRecurrenceAnchor(completedTodo);
        Duration reminderOffset = Duration.between(anchor, completedTodo.getRemindAt());
        return nextTriggerTime.plus(reminderOffset);
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
        return TodoCacheKeys.todoItem(currentUserProvider.getCurrentUserId(), id);
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
    private Specification<TodoItem> buildSpecification(TodoQueryRequest queryRequest, Long userId) {
        String normalizedRecurrenceTypeFilter = normalizeOptionalRecurrenceFilter(queryRequest.getRecurrenceType());
        String normalizedTimePreset = normalizeTimePreset(queryRequest.getTimePreset());

        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(criteriaBuilder.equal(root.get("user").get("id"), userId));

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

            if (normalizedRecurrenceTypeFilter != null) {
                predicates.add(criteriaBuilder.equal(root.get("recurrenceType"), normalizedRecurrenceTypeFilter));
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

            if (queryRequest.getRemindDateFrom() != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(
                        root.get("remindAt"),
                        queryRequest.getRemindDateFrom().atStartOfDay()
                ));
            }

            if (queryRequest.getRemindDateTo() != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(
                        root.get("remindAt"),
                        queryRequest.getRemindDateTo().atTime(23, 59, 59)
                ));
            }

            if (normalizedTimePreset != null) {
                LocalDateTime now = LocalDateTime.now();
                switch (normalizedTimePreset) {
                    case "DUE_TODAY" -> {
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("dueDate"), LocalDate.now().atStartOfDay()));
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("dueDate"), LocalDate.now().atTime(23, 59, 59)));
                    }
                    case "OVERDUE" -> {
                        predicates.add(criteriaBuilder.notEqual(root.get("status"), "DONE"));
                        predicates.add(criteriaBuilder.isNotNull(root.get("dueDate")));
                        predicates.add(criteriaBuilder.lessThan(root.get("dueDate"), now));
                    }
                    case "UPCOMING_REMINDER" -> {
                        predicates.add(criteriaBuilder.notEqual(root.get("status"), "DONE"));
                        predicates.add(criteriaBuilder.isNotNull(root.get("remindAt")));
                        predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("remindAt"), now));
                        predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("remindAt"), now.plusHours(24)));
                    }
                    default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported timePreset: " + normalizedTimePreset);
                }
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
     * 规范化可选的重复类型筛选值。
     *
     * @param recurrenceType 原始重复类型筛选值
     * @return 规范化后的重复类型；未提供时返回 null
     */
    private String normalizeOptionalRecurrenceFilter(String recurrenceType) {
        if (!hasText(recurrenceType)) {
            return null;
        }

        String normalizedRecurrenceType = recurrenceType.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_RECURRENCE_TYPES.contains(normalizedRecurrenceType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recurrenceType must be one of: " + ALLOWED_RECURRENCE_TYPES);
        }
        return normalizedRecurrenceType;
    }

    /**
     * 规范化时间预设筛选值。
     *
     * @param timePreset 原始时间预设值
     * @return 规范化后的时间预设；未提供时返回 null
     */
    private String normalizeTimePreset(String timePreset) {
        if (!hasText(timePreset)) {
            return null;
        }

        String normalizedTimePreset = timePreset.trim().toUpperCase(Locale.ROOT);
        if (!ALLOWED_TIME_PRESETS.contains(normalizedTimePreset)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "timePreset must be one of: " + ALLOWED_TIME_PRESETS);
        }
        return normalizedTimePreset;
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
     * 查询当前请求对应的用户实体，用于在创建任务时建立明确的归属关系。
     *
     * @return 当前登录用户实体
     */
    private AppUser getCurrentUserEntity() {
        Long userId = currentUserProvider.getCurrentUserId();
        return appUserRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required"));
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
     * 规范化统计面板中的分类名称，空分类统一映射为未分类占位键。
     *
     * @param category 原始分类值
     * @return 规范化后的分类名称
     */
    private String normalizeStatsCategory(String category) {
        if (!hasText(category)) {
            return UNCLASSIFIED_CATEGORY_KEY;
        }
        return category.trim();
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
     * 规范化附件链接列表，移除空白行与重复值，最终按换行拼接存储。
     *
     * @param attachmentLinks 原始附件链接字符串
     * @return 规范化后的附件链接字符串
     */
    private String normalizeAttachmentLinks(String attachmentLinks) {
        if (!hasText(attachmentLinks)) {
            return null;
        }

        List<String> normalizedLinks = List.of(attachmentLinks.split("\\R"))
                .stream()
                .map(String::trim)
                .filter(this::hasText)
                .distinct()
                .collect(Collectors.toList());

        if (normalizedLinks.isEmpty()) {
            return null;
        }

        return String.join("\n", normalizedLinks);
    }

    /**
     * 规范化协作人/观察者列表，按逗号分隔去重存储。
     *
     * @param rawPeople 原始人员列表
     * @return 规范化后的逗号分隔字符串
     */
    private String normalizePeopleList(String rawPeople) {
        if (!hasText(rawPeople)) {
            return null;
        }

        List<String> normalizedPeople = List.of(rawPeople.split(","))
                .stream()
                .map(String::trim)
                .filter(this::hasText)
                .distinct()
                .collect(Collectors.toList());

        if (normalizedPeople.isEmpty()) {
            return null;
        }

        return String.join(",", normalizedPeople);
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
