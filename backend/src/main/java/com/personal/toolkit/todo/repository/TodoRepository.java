package com.personal.toolkit.todo.repository;

import com.personal.toolkit.todo.entity.TodoItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<TodoItem, Long>, JpaSpecificationExecutor<TodoItem> {

    Optional<TodoItem> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    Optional<TodoItem> findByIdAndUserId(Long id, Long userId);

    boolean existsByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    boolean existsByUserIdAndDeletedAtIsNullAndTitleAndStatusAndPriorityAndDueDateAndRemindAtAndCategoryAndTagsAndRecurrenceTypeAndRecurrenceIntervalAndRecurrenceEndTimeAndNextTriggerTime(
            Long userId,
            String title,
            String status,
            Integer priority,
            LocalDateTime dueDate,
            LocalDateTime remindAt,
            String category,
            String tags,
            String recurrenceType,
            Integer recurrenceInterval,
            LocalDateTime recurrenceEndTime,
            LocalDateTime nextTriggerTime
    );

    List<TodoItem> findAllByUserIdAndIdIn(Long userId, List<Long> ids);

    long countByUserIdAndDeletedAtIsNullAndCompletedAtBetween(Long userId, LocalDateTime start, LocalDateTime end);

    long countByUserIdAndDeletedAtIsNullAndStatusNot(Long userId, String status);

    long countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateBetween(Long userId,
                                                                      String status,
                                                                      LocalDateTime dueDateStart,
                                                                      LocalDateTime dueDateEnd);

    long countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateIsNull(Long userId, String status);

    long countByUserIdAndDeletedAtIsNullAndStatusNotAndDueDateBefore(Long userId, String status, LocalDateTime dueDate);

    long countByUserIdAndDeletedAtIsNullAndStatusNotAndRemindAtBetween(Long userId,
                                                                       String status,
                                                                       LocalDateTime remindAtStart,
                                                                       LocalDateTime remindAtEnd);

    @Query("""
             select t.category,
                    sum(case when t.status <> 'DONE' then 1 else 0 end),
                    sum(case when t.status = 'DONE' then 1 else 0 end)
             from TodoItem t
             where t.user.id = :userId
               and t.deletedAt is null
             group by t.category
             order by lower(coalesce(t.category, '')) asc
             """)
    List<Object[]> summarizeByCategory(@Param("userId") Long userId);

    @Query("""
             select t.completedAt
             from TodoItem t
             where t.user.id = :userId
               and t.deletedAt is null
               and t.completedAt between :start and :end
             """)
    List<LocalDateTime> findCompletedAtBetween(@Param("userId") Long userId,
                                               @Param("start") LocalDateTime start,
                                               @Param("end") LocalDateTime end);

    @Query("""
             select t.createTime
             from TodoItem t
             where t.user.id = :userId
               and t.deletedAt is null
               and t.createTime between :start and :end
             """)
    List<LocalDateTime> findCreatedAtBetween(@Param("userId") Long userId,
                                             @Param("start") LocalDateTime start,
                                             @Param("end") LocalDateTime end);

    @Query("""
             select t.priority,
                    count(t)
             from TodoItem t
             where t.user.id = :userId
               and t.deletedAt is null
               and t.status <> 'DONE'
             group by t.priority
             order by t.priority asc
             """)
    List<Object[]> summarizeActiveByPriority(@Param("userId") Long userId);
}
