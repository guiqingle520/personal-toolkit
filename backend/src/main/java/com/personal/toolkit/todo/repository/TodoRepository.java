package com.personal.toolkit.todo.repository;

import com.personal.toolkit.todo.entity.TodoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<TodoItem, Long>, JpaSpecificationExecutor<TodoItem> {

    Optional<TodoItem> findByIdAndDeletedAtIsNull(Long id);

    boolean existsByIdAndDeletedAtIsNull(Long id);

    boolean existsByDeletedAtIsNullAndTitleAndStatusAndPriorityAndDueDateAndCategoryAndTagsAndRecurrenceTypeAndRecurrenceIntervalAndRecurrenceEndTimeAndNextTriggerTime(
            String title,
            String status,
            Integer priority,
            java.time.LocalDateTime dueDate,
            String category,
            String tags,
            String recurrenceType,
            Integer recurrenceInterval,
            java.time.LocalDateTime recurrenceEndTime,
            java.time.LocalDateTime nextTriggerTime
    );

    List<TodoItem> findAllByIdIn(List<Long> ids);
}
