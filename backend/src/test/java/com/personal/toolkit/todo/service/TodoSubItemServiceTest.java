package com.personal.toolkit.todo.service;

import com.personal.toolkit.todo.dto.TodoSubItemRequest;
import com.personal.toolkit.todo.dto.TodoSubItemSummaryResponse;
import com.personal.toolkit.todo.entity.TodoSubItem;
import com.personal.toolkit.todo.repository.TodoRepository;
import com.personal.toolkit.todo.repository.TodoSubItemRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证 TodoSubItemService 的 checklist CRUD 与聚合摘要行为。
 */
@ExtendWith(MockitoExtension.class)
class TodoSubItemServiceTest {

    @Mock
    private TodoSubItemRepository todoSubItemRepository;

    @Mock
    private TodoRepository todoRepository;

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @InjectMocks
    private TodoSubItemService todoSubItemService;

    /**
     * 创建子任务时应规范化标题、状态和排序值。
     */
    @Test
    void createShouldNormalizeFields() {
        TodoSubItemRequest request = new TodoSubItemRequest();
        request.setTitle("  Write tests  ");
        request.setStatus("done");

        when(todoRepository.existsByIdAndDeletedAtIsNull(1L)).thenReturn(true);
        when(todoSubItemRepository.save(any(TodoSubItem.class))).thenAnswer(invocation -> {
            TodoSubItem entity = invocation.getArgument(0);
            entity.setId(10L);
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            return entity;
        });

        var response = todoSubItemService.create(1L, request);

        assertEquals(10L, response.getId());
        assertEquals("Write tests", response.getTitle());
        assertEquals("DONE", response.getStatus());
        assertEquals(0, response.getSortOrder());
        verify(redisTemplate).delete(TodoCacheKeys.todoItem(1L));
    }

    /**
     * 主任务不存在时应拒绝创建子任务。
     */
    @Test
    void createShouldFailWhenParentTodoMissing() {
        TodoSubItemRequest request = new TodoSubItemRequest();
        request.setTitle("Sub item");

        when(todoRepository.existsByIdAndDeletedAtIsNull(99L)).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> todoSubItemService.create(99L, request));

        assertEquals(404, exception.getStatusCode().value());
        verify(todoSubItemRepository, never()).save(any(TodoSubItem.class));
    }

    /**
     * 更新子任务时应校验归属关系并持久化变更。
     */
    @Test
    void updateShouldPersistChecklistChanges() {
        TodoSubItemRequest request = new TodoSubItemRequest();
        request.setTitle("Review API contract");
        request.setStatus("PENDING");
        request.setSortOrder(3);

        TodoSubItem existing = createSubItem(5L, 1L, "Old", "DONE", 0);
        when(todoRepository.existsByIdAndDeletedAtIsNull(1L)).thenReturn(true);
        when(todoSubItemRepository.findByIdAndTodoId(5L, 1L)).thenReturn(Optional.of(existing));
        when(todoSubItemRepository.save(any(TodoSubItem.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = todoSubItemService.update(1L, 5L, request);

        assertEquals("Review API contract", response.getTitle());
        assertEquals("PENDING", response.getStatus());
        assertEquals(3, response.getSortOrder());
        verify(redisTemplate).delete(TodoCacheKeys.todoItem(1L));
    }

    /**
     * 删除子任务时应执行目标记录删除。
     */
    @Test
    void deleteShouldRemoveSubItem() {
        TodoSubItem existing = createSubItem(6L, 2L, "Delete me", "PENDING", 1);
        when(todoRepository.existsByIdAndDeletedAtIsNull(2L)).thenReturn(true);
        when(todoSubItemRepository.findByIdAndTodoId(6L, 2L)).thenReturn(Optional.of(existing));

        todoSubItemService.delete(2L, 6L);

        verify(todoSubItemRepository).delete(existing);
        verify(redisTemplate).delete(TodoCacheKeys.todoItem(2L));
    }

    /**
     * 摘要接口应返回总数、完成数与进度百分比。
     */
    @Test
    void getSummaryShouldReturnProgressAggregate() {
        when(todoRepository.existsByIdAndDeletedAtIsNull(3L)).thenReturn(true);
        when(todoSubItemRepository.findAllByTodoIdOrderBySortOrderAscIdAsc(3L)).thenReturn(List.of(
                createSubItem(1L, 3L, "A", "DONE", 0),
                createSubItem(2L, 3L, "B", "PENDING", 1),
                createSubItem(3L, 3L, "C", "DONE", 2)
        ));

        TodoSubItemSummaryResponse response = todoSubItemService.getSummary(3L);

        assertEquals(3, response.getTotalCount());
        assertEquals(2, response.getCompletedCount());
        assertEquals(66, response.getProgressPercent());
    }

    private TodoSubItem createSubItem(Long id, Long todoId, String title, String status, Integer sortOrder) {
        TodoSubItem entity = new TodoSubItem();
        entity.setId(id);
        entity.setTodoId(todoId);
        entity.setTitle(title);
        entity.setStatus(status);
        entity.setSortOrder(sortOrder);
        entity.setCreateTime(LocalDateTime.now());
        entity.setUpdateTime(LocalDateTime.now());
        return entity;
    }
}
