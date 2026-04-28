package com.personal.toolkit.todo.repository;

import com.personal.toolkit.todo.entity.TodoReminderEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TodoReminderEventRepository extends JpaRepository<TodoReminderEvent, Long> {

    Optional<TodoReminderEvent> findByIdAndUserId(Long id, Long userId);

    Optional<TodoReminderEvent> findByDedupeKey(String dedupeKey);

    List<TodoReminderEvent> findByTodoIdAndStatusIn(Long todoId, Collection<String> statuses);

    List<TodoReminderEvent> findByStatusAndScheduledAtLessThanEqual(String status, LocalDateTime scheduledAt);

    List<TodoReminderEvent> findByUserIdAndStatus(Long userId, String status);

    Page<TodoReminderEvent> findByUserId(Long userId, Pageable pageable);

    Page<TodoReminderEvent> findByUserIdAndStatus(Long userId, String status, Pageable pageable);

    long countByUserIdAndStatus(Long userId, String status);

    long countByUserIdAndStatusAndReadAtBetween(Long userId,
                                                String status,
                                                LocalDateTime readAtStart,
                                                LocalDateTime readAtEnd);

    long countByUserIdAndStatusAndScheduledAtLessThan(Long userId, String status, LocalDateTime scheduledAt);
}
