package com.personal.toolkit.todo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.toolkit.common.exception.GlobalExceptionHandler;
import com.personal.toolkit.todo.dto.PageResponse;
import com.personal.toolkit.todo.dto.TodoBatchRequest;
import com.personal.toolkit.todo.dto.TodoItemRequest;
import com.personal.toolkit.todo.dto.TodoOptionResponse;
import com.personal.toolkit.todo.dto.TodoSubItemSummaryResponse;
import com.personal.toolkit.todo.entity.TodoItem;
import com.personal.toolkit.todo.service.TodoService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 验证 TodoController 暴露的第二阶段接口契约与异常返回结构。
 */
@WebMvcTest(TodoController.class)
@Import(GlobalExceptionHandler.class)
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TodoService todoService;

    /**
     * 查询分类标签候选项时应返回成功响应体。
     */
    @Test
    void getOptionsShouldReturnOptionPayload() throws Exception {
        when(todoService.getOptions()).thenReturn(new TodoOptionResponse(List.of("Work"), List.of("urgent")));

        mockMvc.perform(get("/api/todos/options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Todo options fetched successfully"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.categories[0]").value("Work"))
                .andExpect(jsonPath("$.data.tags[0]").value("urgent"));
    }

    /**
     * 活动列表查询接口应返回前端依赖的统一分页响应结构。
     */
    @Test
    void findAllShouldReturnPagedTodoContract() throws Exception {
        PageResponse<TodoItem> pageResponse = PageResponse.from(new PageImpl<>(
                List.of(createTodo(1L, "PENDING")),
                PageRequest.of(0, 10, Sort.by("createTime")),
                1
        ));
        when(todoService.findAll(any(), eq(0), eq(10), any(Sort.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/todos")
                        .param("status", "PENDING")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Todo list fetched successfully"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.content[0].id").value(1L))
                .andExpect(jsonPath("$.data.content[0].title").value("Todo-1"))
                .andExpect(jsonPath("$.data.content[0].status").value("PENDING"))
                .andExpect(jsonPath("$.data.content[0].subItemSummary.totalCount").value(3))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.first").value(true))
                .andExpect(jsonPath("$.data.last").value(true));
    }

    /**
     * 详情接口应返回前端编辑和状态切换所需字段。
     */
    @Test
    void findByIdShouldReturnTodoDetailContract() throws Exception {
        TodoItem todo = createTodo(3L, "DONE");
        todo.setCategory("Work");
        todo.setTags("backend,urgent");
        when(todoService.findById(3L)).thenReturn(todo);

        mockMvc.perform(get("/api/todos/3"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Todo item fetched successfully"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(3L))
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.data.category").value("Work"))
                .andExpect(jsonPath("$.data.recurrenceType").value("DAILY"))
                .andExpect(jsonPath("$.data.subItemSummary.completedCount").value(2))
                .andExpect(jsonPath("$.data.tags").value("backend,urgent"));
    }

    /**
     * 批量完成接口应返回更新后的待办事项列表。
     */
    @Test
    void batchCompleteShouldReturnUpdatedTodos() throws Exception {
        TodoItem todo = createTodo(1L, "DONE");
        when(todoService.batchComplete(anyList())).thenReturn(List.of(todo));

        TodoBatchRequest request = new TodoBatchRequest();
        request.setIds(List.of(1L));

        mockMvc.perform(post("/api/todos/batch/complete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Todo items completed successfully"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data[0].status").value("DONE"));
    }

    /**
     * 查询回收站时应返回分页结构。
     */
    @Test
    void recycleBinShouldReturnPageResponse() throws Exception {
        PageResponse<TodoItem> pageResponse = PageResponse.from(new PageImpl<>(
                List.of(createTodo(9L, "DONE")),
                PageRequest.of(0, 10, Sort.by("updateTime")),
                1
        ));
        when(todoService.findAll(any(), eq(0), eq(10), any(Sort.class))).thenReturn(pageResponse);

        mockMvc.perform(get("/api/todos/recycle-bin"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Recycle bin fetched successfully"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.content[0].id").value(9L));
    }

    /**
     * 单个恢复接口应返回恢复后的待办事项。
     */
    @Test
    void restoreShouldReturnTodo() throws Exception {
        when(todoService.restore(5L)).thenReturn(createTodo(5L, "PENDING"));

        mockMvc.perform(put("/api/todos/5/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Todo item restored successfully"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(5L));
    }

    /**
     * 创建接口成功时应返回 201 和完整 ApiResponse 外壳。
     */
    @Test
    void createShouldReturnCreatedTodoContract() throws Exception {
        TodoItemRequest request = new TodoItemRequest();
        request.setTitle("Create docs");
        request.setStatus("PENDING");
        request.setPriority(3);
        request.setDueDate(java.time.LocalDateTime.of(2026, 4, 10, 9, 0));
        request.setRecurrenceType("DAILY");
        request.setRecurrenceInterval(1);

        when(todoService.create(any(TodoItemRequest.class))).thenReturn(createTodo(10L, "PENDING"));

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Todo item created successfully"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(10L))
                .andExpect(jsonPath("$.data.recurrenceType").value("DAILY"))
                .andExpect(jsonPath("$.data.title").value("Todo-10"));
    }

    /**
     * 更新接口成功时应返回统一响应结构和更新后的任务字段。
     */
    @Test
    void updateShouldReturnUpdatedTodoContract() throws Exception {
        TodoItemRequest request = new TodoItemRequest();
        request.setTitle("Update docs");
        request.setStatus("DONE");
        request.setPriority(4);
        request.setDueDate(java.time.LocalDateTime.of(2026, 4, 12, 9, 0));
        request.setRecurrenceType("WEEKLY");
        request.setRecurrenceInterval(1);

        TodoItem updated = createTodo(11L, "DONE");
        updated.setCategory("Work");
        when(todoService.update(eq(11L), any(TodoItemRequest.class))).thenReturn(updated);

        mockMvc.perform(put("/api/todos/11")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Todo item updated successfully"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data.id").value(11L))
                .andExpect(jsonPath("$.data.status").value("DONE"))
                .andExpect(jsonPath("$.data.recurrenceType").value("WEEKLY"))
                .andExpect(jsonPath("$.data.category").value("Work"));
    }

    /**
     * 批量删除接口应返回成功响应体。
     */
    @Test
    void batchDeleteShouldReturnSuccess() throws Exception {
        doNothing().when(todoService).batchDelete(anyList());
        TodoBatchRequest request = new TodoBatchRequest();
        request.setIds(List.of(1L, 2L));

        mockMvc.perform(post("/api/todos/batch/delete")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Todo items deleted successfully"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    /**
     * 删除接口成功时应返回无 data 的统一成功响应。
     */
    @Test
    void deleteShouldReturnSuccessEnvelope() throws Exception {
        doNothing().when(todoService).delete(12L);

        mockMvc.perform(delete("/api/todos/12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Todo item deleted successfully"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    /**
     * 批量恢复接口应返回恢复后的任务列表契约。
     */
    @Test
    void batchRestoreShouldReturnRestoredTodos() throws Exception {
        TodoBatchRequest request = new TodoBatchRequest();
        request.setIds(List.of(1L, 2L));
        when(todoService.batchRestore(anyList())).thenReturn(List.of(createTodo(1L, "PENDING")));

        mockMvc.perform(post("/api/todos/batch/restore")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Todo items restored successfully"))
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.data[0].id").value(1L));
    }

    /**
     * 校验失败时应通过全局异常处理器返回字段级错误结构。
     */
    @Test
    void createShouldReturnValidationError() throws Exception {
        TodoItemRequest request = new TodoItemRequest();
        request.setStatus("PENDING");
        request.setPriority(3);

        mockMvc.perform(post("/api/todos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.validation.title[0]").exists());
    }

    /**
     * 业务异常应被转换为统一错误响应。
     */
    @Test
    void restoreShouldUseGlobalErrorResponseWhenServiceFails() throws Exception {
        when(todoService.restore(99L)).thenThrow(new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "Todo item not found: 99"));

        mockMvc.perform(put("/api/todos/99/restore"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Todo item not found: 99"));
    }

    private TodoItem createTodo(Long id, String status) {
        TodoItem todo = new TodoItem();
        todo.setId(id);
        todo.setTitle("Todo-" + id);
        todo.setStatus(status);
        todo.setPriority(3);
        todo.setRecurrenceType(id == 11L ? "WEEKLY" : "DAILY");
        todo.setRecurrenceInterval(1);
        todo.setNextTriggerTime(java.time.LocalDateTime.of(2026, 4, 10, 9, 0));
        todo.setSubItemSummary(new TodoSubItemSummaryResponse(3, 2, 66));
        return todo;
    }
}
