package com.personal.toolkit.todo.repository;

import com.personal.toolkit.todo.entity.TodoSavedView;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TodoSavedViewRepository extends JpaRepository<TodoSavedView, Long> {

    List<TodoSavedView> findByUserIdOrderByCreateTimeAsc(Long userId);

    Optional<TodoSavedView> findByIdAndUserId(Long id, Long userId);

    Optional<TodoSavedView> findByUserIdAndDefaultViewTrue(Long userId);

    boolean existsByUserIdAndNameIgnoreCase(Long userId, String name);

    boolean existsByUserIdAndNameIgnoreCaseAndIdNot(Long userId, String name, Long id);
}
