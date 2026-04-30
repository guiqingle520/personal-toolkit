package com.personal.toolkit.todo.service;

import com.personal.toolkit.auth.security.CurrentUserProvider;
import com.personal.toolkit.todo.dto.PageResponse;
import com.personal.toolkit.todo.dto.TodoReminderItemResponse;
import com.personal.toolkit.todo.dto.TodoReminderQueryRequest;
import com.personal.toolkit.todo.dto.TodoReminderSummaryResponse;
import com.personal.toolkit.todo.dto.TodoReminderStatsResponse;
import com.personal.toolkit.todo.entity.TodoItem;
import com.personal.toolkit.todo.entity.TodoReminderEvent;
import com.personal.toolkit.todo.repository.TodoReminderEventRepository;
import com.personal.toolkit.todo.repository.TodoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 处理 Todo 站内提醒事件的生成、查询与已读状态变更。
 */
@Service
public class TodoReminderService {

    private static final Logger log = LoggerFactory.getLogger(TodoReminderService.class);

    private static final Set<String> QUERYABLE_STATUSES = Set.of(
            TodoReminderEvent.STATUS_PENDING,
            TodoReminderEvent.STATUS_SENT,
            TodoReminderEvent.STATUS_READ,
            TodoReminderEvent.STATUS_CANCELLED
    );

    private final TodoReminderEventRepository todoReminderEventRepository;
    private final TodoRepository todoRepository;
    private final CurrentUserProvider currentUserProvider;

    public TodoReminderService(TodoReminderEventRepository todoReminderEventRepository,
                               TodoRepository todoRepository,
                               CurrentUserProvider currentUserProvider) {
        this.todoReminderEventRepository = todoReminderEventRepository;
        this.todoRepository = todoRepository;
        this.currentUserProvider = currentUserProvider;
    }

    /**
     * 根据 Todo 当前状态同步待发送提醒事件。
     *
     * @param todoItem 待同步的 Todo 实体
     */
    @Transactional
    public void syncReminderForTodo(TodoItem todoItem) {
        if (todoItem == null || todoItem.getId() == null || todoItem.getUser() == null || todoItem.getUser().getId() == null) {
            return;
        }

        List<TodoReminderEvent> pendingEvents;
        try {
            pendingEvents = todoReminderEventRepository.findByTodoIdAndStatusIn(
                    todoItem.getId(),
                    List.of(TodoReminderEvent.STATUS_PENDING)
            );
        } catch (InvalidDataAccessResourceUsageException ex) {
            logMissingReminderTable("syncing reminder for todo " + todoItem.getId());
            return;
        }

        if (todoItem.getDeletedAt() != null || todoItem.getRemindAt() == null || "DONE".equals(todoItem.getStatus())) {
            cancelPendingEvents(pendingEvents);
            return;
        }

        String dedupeKey = buildDedupeKey(todoItem);
        pendingEvents.stream()
                .filter(event -> !dedupeKey.equals(event.getDedupeKey()))
                .forEach(TodoReminderEvent::markCancelled);
        try {
            todoReminderEventRepository.saveAll(pendingEvents);
        } catch (InvalidDataAccessResourceUsageException ex) {
            logMissingReminderTable("syncing reminder for todo " + todoItem.getId());
            return;
        }

        try {
            todoReminderEventRepository.findByDedupeKey(dedupeKey).ifPresentOrElse(existingEvent -> {
                if (TodoReminderEvent.STATUS_CANCELLED.equals(existingEvent.getStatus())) {
                    existingEvent.reactivate(todoItem.getRemindAt());
                    todoReminderEventRepository.save(existingEvent);
                }
            }, () -> {
                TodoReminderEvent reminderEvent = new TodoReminderEvent();
                reminderEvent.setTodoId(todoItem.getId());
                reminderEvent.setUserId(todoItem.getUser().getId());
                reminderEvent.setScheduledAt(todoItem.getRemindAt());
                reminderEvent.setStatus(TodoReminderEvent.STATUS_PENDING);
                reminderEvent.setDedupeKey(dedupeKey);
                todoReminderEventRepository.save(reminderEvent);
            });
        } catch (InvalidDataAccessResourceUsageException ex) {
            logMissingReminderTable("syncing reminder for todo " + todoItem.getId());
        }
    }

    /**
     * 将指定 Todo 尚未发送的提醒事件统一取消。
     *
     * @param todoId Todo 主键
     */
    @Transactional
    public void cancelPendingRemindersForTodo(Long todoId) {
        try {
            cancelPendingEvents(todoReminderEventRepository.findByTodoIdAndStatusIn(
                    todoId,
                    List.of(TodoReminderEvent.STATUS_PENDING)
            ));
        } catch (InvalidDataAccessResourceUsageException ex) {
            logMissingReminderTable("cancelling reminders for todo " + todoId);
        }
    }

    /**
     * 扫描已到提醒时间的待发送事件，并将其置为已发送状态。
     */
    @Transactional
    public void generateDueReminderEvents() {
        LocalDateTime now = LocalDateTime.now();
        List<TodoReminderEvent> readyEvents = todoReminderEventRepository.findByStatusAndScheduledAtLessThanEqual(
                TodoReminderEvent.STATUS_PENDING,
                now
        );

        readyEvents.forEach(event -> event.markSent(now));
        todoReminderEventRepository.saveAll(readyEvents);
    }

    /**
     * 分页查询当前用户的提醒列表。
     *
     * @param request 查询参数
     * @return 提醒分页结果
     */
    @Transactional(readOnly = true)
    public PageResponse<TodoReminderItemResponse> queryReminderEvents(TodoReminderQueryRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        String normalizedStatus = normalizeQueryStatus(request.getStatus());
        int page = Math.max(request.getPage(), 0);
        int size = request.getSize() <= 0 ? 10 : Math.min(request.getSize(), 50);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "scheduledAt"));

        Page<TodoReminderEvent> reminderPage;
        try {
            reminderPage = normalizedStatus == null
                    ? todoReminderEventRepository.findByUserId(userId, pageable)
                    : todoReminderEventRepository.findByUserIdAndStatus(userId, normalizedStatus, pageable);
        } catch (InvalidDataAccessResourceUsageException ex) {
            logMissingReminderTable("querying reminders for user " + userId);
            return PageResponse.from(new PageImpl<>(List.of(), pageable, 0));
        }

        List<Long> todoIds = reminderPage.getContent().stream()
                .map(TodoReminderEvent::getTodoId)
                .distinct()
                .collect(Collectors.toList());
        Map<Long, TodoItem> todoById = todoIds.isEmpty()
                ? Map.of()
                : todoRepository.findAllByUserIdAndIdIn(userId, todoIds).stream()
                .collect(Collectors.toMap(TodoItem::getId, Function.identity()));

        List<TodoReminderItemResponse> items = reminderPage.getContent().stream()
                .map(event -> toReminderItemResponse(event, todoById.get(event.getTodoId())))
                .collect(Collectors.toList());

        return PageResponse.from(new PageImpl<>(items, pageable, reminderPage.getTotalElements()));
    }

    /**
     * 返回当前用户的提醒统计信息。
     *
     * @return 提醒统计数据
     */
    @Transactional(readOnly = true)
    public TodoReminderStatsResponse getReminderStats() {
        return new TodoReminderStatsResponse(countUnreadReminders(currentUserProvider.getCurrentUserId()));
    }

    /**
     * 返回当前用户更完整的提醒摘要统计信息。
     *
     * @return 提醒摘要统计数据
     */
    @Transactional(readOnly = true)
    public TodoReminderSummaryResponse getReminderSummary() {
        Long userId = currentUserProvider.getCurrentUserId();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfToday = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfToday = now.toLocalDate().atTime(23, 59, 59);

        try {
            long unreadCount = todoReminderEventRepository.countByUserIdAndStatus(userId, TodoReminderEvent.STATUS_SENT);
            long readTodayCount = todoReminderEventRepository.countByUserIdAndStatusAndReadAtBetween(
                    userId,
                    TodoReminderEvent.STATUS_READ,
                    startOfToday,
                    endOfToday
            );
            long scheduledCount = todoReminderEventRepository.countByUserIdAndStatus(userId, TodoReminderEvent.STATUS_PENDING);
            long overdueReminderCount = todoReminderEventRepository.countByUserIdAndStatusAndScheduledAtLessThan(
                    userId,
                    TodoReminderEvent.STATUS_PENDING,
                    now
            );

            return new TodoReminderSummaryResponse(unreadCount, readTodayCount, scheduledCount, overdueReminderCount);
        } catch (InvalidDataAccessResourceUsageException ex) {
            logMissingReminderTable("querying reminder summary for user " + userId);
            return new TodoReminderSummaryResponse(0, 0, 0, 0);
        }
    }

    /**
     * 将当前用户的一条提醒标记为已读。
     *
     * @param reminderId 提醒主键
     */
    @Transactional
    public void markReminderAsRead(Long reminderId) {
        TodoReminderEvent reminderEvent;
        try {
            reminderEvent = todoReminderEventRepository.findByIdAndUserId(reminderId, currentUserProvider.getCurrentUserId())
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo reminder not found: " + reminderId));
        } catch (InvalidDataAccessResourceUsageException ex) {
            logMissingReminderTable("marking reminder as read " + reminderId);
            return;
        }
        if (TodoReminderEvent.STATUS_SENT.equals(reminderEvent.getStatus())) {
            reminderEvent.markRead(LocalDateTime.now());
            try {
                todoReminderEventRepository.save(reminderEvent);
            } catch (InvalidDataAccessResourceUsageException ex) {
                logMissingReminderTable("marking reminder as read " + reminderId);
            }
        }
    }

    /**
     * 将当前用户全部未读提醒统一标记为已读。
     */
    @Transactional
    public void markAllRemindersAsRead() {
        LocalDateTime now = LocalDateTime.now();
        List<TodoReminderEvent> unreadEvents;
        try {
            unreadEvents = todoReminderEventRepository.findByUserIdAndStatus(
                    currentUserProvider.getCurrentUserId(),
                    TodoReminderEvent.STATUS_SENT
            );
        } catch (InvalidDataAccessResourceUsageException ex) {
            logMissingReminderTable("marking all reminders as read");
            return;
        }
        unreadEvents.forEach(event -> event.markRead(now));
        try {
            todoReminderEventRepository.saveAll(unreadEvents);
        } catch (InvalidDataAccessResourceUsageException ex) {
            logMissingReminderTable("marking all reminders as read");
        }
    }

    /**
     * 统计指定用户当前未读提醒数量。
     *
     * @param userId 用户主键
     * @return 未读提醒数量
     */
    @Transactional(readOnly = true)
    public long countUnreadReminders(Long userId) {
        try {
            return todoReminderEventRepository.countByUserIdAndStatus(userId, TodoReminderEvent.STATUS_SENT);
        } catch (InvalidDataAccessResourceUsageException ex) {
            logMissingReminderTable("counting unread reminders for user " + userId);
            return 0;
        }
    }

    private void logMissingReminderTable(String action) {
        log.warn("Skipping reminder operation while {} because reminder event table is unavailable. Run backend/sql/alter_todo_item_phase7_reminder_events.sql to enable reminder delivery.", action);
    }

    private void cancelPendingEvents(List<TodoReminderEvent> pendingEvents) {
        if (pendingEvents == null || pendingEvents.isEmpty()) {
            return;
        }
        pendingEvents.forEach(TodoReminderEvent::markCancelled);
        todoReminderEventRepository.saveAll(pendingEvents);
    }

    private String buildDedupeKey(TodoItem todoItem) {
        return todoItem.getId() + ":" + todoItem.getRemindAt();
    }

    private String normalizeQueryStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String normalizedStatus = status.trim().toUpperCase(Locale.ROOT);
        if (!QUERYABLE_STATUSES.contains(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "status must be one of: " + QUERYABLE_STATUSES);
        }
        return normalizedStatus;
    }

    private TodoReminderItemResponse toReminderItemResponse(TodoReminderEvent event, TodoItem todoItem) {
        return new TodoReminderItemResponse(
                event.getId(),
                event.getTodoId(),
                todoItem == null ? "Todo #" + event.getTodoId() : todoItem.getTitle(),
                todoItem == null ? null : todoItem.getStatus(),
                todoItem == null ? null : todoItem.getCategory(),
                todoItem == null ? null : todoItem.getDueDate(),
                event.getScheduledAt(),
                event.getStatus(),
                event.getSentAt(),
                event.getReadAt()
        );
    }
}
