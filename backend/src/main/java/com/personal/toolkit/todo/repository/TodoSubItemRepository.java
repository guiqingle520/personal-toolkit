package com.personal.toolkit.todo.repository;

import com.personal.toolkit.todo.entity.TodoSubItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/**
 * 提供子任务记录的持久化查询能力。
 */
public interface TodoSubItemRepository extends JpaRepository<TodoSubItem, Long> {

    List<TodoSubItem> findAllByTodoIdOrderBySortOrderAscIdAsc(Long todoId);

    Optional<TodoSubItem> findByIdAndTodoId(Long id, Long todoId);

    @Query("""
            select s.todoId, count(s), sum(case when s.status = 'DONE' then 1 else 0 end)
            from TodoSubItem s
            where s.todoId in :todoIds
            group by s.todoId
            """)
    List<Object[]> summarizeByTodoIds(@Param("todoIds") List<Long> todoIds);
}
