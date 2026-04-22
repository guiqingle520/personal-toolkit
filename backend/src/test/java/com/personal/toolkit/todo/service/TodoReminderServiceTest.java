package com.personal.toolkit.todo.service;

import com.personal.toolkit.auth.entity.AppUser;
import com.personal.toolkit.auth.security.CurrentUserProvider;
import com.personal.toolkit.todo.dto.PageResponse;
import com.personal.toolkit.todo.dto.TodoReminderQueryRequest;
import com.personal.toolkit.todo.entity.TodoItem;
import com.personal.toolkit.todo.entity.TodoReminderEvent;
import com.personal.toolkit.todo.repository.TodoReminderEventRepository;
import com.personal.toolkit.todo.repository.TodoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.InvalidDataAccessResourceUsageException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodoReminderServiceTest {

    private static final Long USER_ID = 101L;

    @Mock
    private TodoReminderEventRepository todoReminderEventRepository;

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private TodoReminderService todoReminderService;

    @BeforeEach
    void setUp() {
        todoReminderService = new TodoReminderService(todoReminderEventRepository, todoRepository, currentUserProvider);
    }

    @Test
    void syncReminderForTodoShouldCreatePendingEvent() {
        TodoItem todoItem = createTodo(1L, "PENDING", LocalDateTime.of(2026, 4, 22, 20, 30));
        when(todoReminderEventRepository.findByTodoIdAndStatusIn(eq(1L), any())).thenReturn(List.of());
        when(todoReminderEventRepository.findByDedupeKey("1:2026-04-22T20:30")).thenReturn(Optional.empty());

        todoReminderService.syncReminderForTodo(todoItem);

        ArgumentCaptor<TodoReminderEvent> captor = ArgumentCaptor.forClass(TodoReminderEvent.class);
        verify(todoReminderEventRepository).save(captor.capture());
        assertEquals(1L, captor.getValue().getTodoId());
        assertEquals(USER_ID, captor.getValue().getUserId());
        assertEquals(TodoReminderEvent.STATUS_PENDING, captor.getValue().getStatus());
    }

    @Test
    void syncReminderForTodoShouldCancelPendingEventWhenTodoDone() {
        TodoItem todoItem = createTodo(2L, "DONE", LocalDateTime.of(2026, 4, 22, 20, 30));
        TodoReminderEvent pendingEvent = createReminderEvent(21L, 2L, TodoReminderEvent.STATUS_PENDING);
        when(todoReminderEventRepository.findByTodoIdAndStatusIn(eq(2L), any())).thenReturn(List.of(pendingEvent));

        todoReminderService.syncReminderForTodo(todoItem);

        assertEquals(TodoReminderEvent.STATUS_CANCELLED, pendingEvent.getStatus());
        verify(todoReminderEventRepository).saveAll(List.of(pendingEvent));
        verify(todoReminderEventRepository, never()).save(any(TodoReminderEvent.class));
    }

    @Test
    void generateDueReminderEventsShouldMarkPendingEventsAsSent() {
        TodoReminderEvent pendingEvent = createReminderEvent(31L, 3L, TodoReminderEvent.STATUS_PENDING);
        pendingEvent.setScheduledAt(LocalDateTime.now().minusMinutes(1));
        when(todoReminderEventRepository.findByStatusAndScheduledAtLessThanEqual(eq(TodoReminderEvent.STATUS_PENDING), any(LocalDateTime.class)))
                .thenReturn(List.of(pendingEvent));

        todoReminderService.generateDueReminderEvents();

        assertEquals(TodoReminderEvent.STATUS_SENT, pendingEvent.getStatus());
        verify(todoReminderEventRepository).saveAll(List.of(pendingEvent));
    }

    @Test
    void queryReminderEventsShouldMapTodoFields() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        TodoReminderEvent reminderEvent = createReminderEvent(41L, 4L, TodoReminderEvent.STATUS_SENT);
        reminderEvent.setScheduledAt(LocalDateTime.of(2026, 4, 22, 20, 30));
        TodoItem todoItem = createTodo(4L, "PENDING", LocalDateTime.of(2026, 4, 22, 20, 30));
        todoItem.setTitle("Review logs");
        todoItem.setCategory("Work");
        when(todoReminderEventRepository.findByUserIdAndStatus(eq(USER_ID), eq(TodoReminderEvent.STATUS_SENT), any(PageRequest.class)))
                .thenReturn(new PageImpl<>(List.of(reminderEvent), PageRequest.of(0, 10), 1));
        when(todoRepository.findAllByUserIdAndIdIn(USER_ID, List.of(4L))).thenReturn(List.of(todoItem));

        TodoReminderQueryRequest request = new TodoReminderQueryRequest();
        request.setStatus("SENT");
        PageResponse<?> response = todoReminderService.queryReminderEvents(request);

        assertEquals(1, response.getContent().size());
        assertEquals("Review logs", ((com.personal.toolkit.todo.dto.TodoReminderItemResponse) response.getContent().get(0)).getTodoTitle());
    }

    @Test
    void markReminderAsReadShouldRejectOtherUsersEvent() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(todoReminderEventRepository.findByIdAndUserId(99L, USER_ID)).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> todoReminderService.markReminderAsRead(99L));

        assertEquals(404, exception.getStatusCode().value());
    }

    @Test
    void syncReminderForTodoShouldSkipWhenReminderTableMissing() {
        TodoItem todoItem = createTodo(5L, "PENDING", LocalDateTime.of(2026, 4, 22, 20, 30));
        when(todoReminderEventRepository.findByTodoIdAndStatusIn(eq(5L), any()))
                .thenThrow(new InvalidDataAccessResourceUsageException("missing table"));

        todoReminderService.syncReminderForTodo(todoItem);

        verify(todoReminderEventRepository, never()).save(any(TodoReminderEvent.class));
    }

    private TodoItem createTodo(Long id, String status, LocalDateTime remindAt) {
        TodoItem todoItem = new TodoItem();
        AppUser user = new AppUser();
        user.setId(USER_ID);
        todoItem.setId(id);
        todoItem.setUser(user);
        todoItem.setTitle("Todo-" + id);
        todoItem.setStatus(status);
        todoItem.setPriority(3);
        todoItem.setRemindAt(remindAt);
        todoItem.setDueDate(remindAt == null ? null : remindAt.plusHours(1));
        return todoItem;
    }

    private TodoReminderEvent createReminderEvent(Long id, Long todoId, String status) {
        TodoReminderEvent reminderEvent = new TodoReminderEvent();
        reminderEvent.setId(id);
        reminderEvent.setTodoId(todoId);
        reminderEvent.setUserId(USER_ID);
        reminderEvent.setStatus(status);
        reminderEvent.setDedupeKey(todoId + ":dedupe");
        return reminderEvent;
    }
}
