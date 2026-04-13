package com.personal.toolkit.todo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.toolkit.auth.config.SecurityConfig;
import com.personal.toolkit.auth.security.AppUserDetailsService;
import com.personal.toolkit.auth.security.JwtAuthenticationFilter;
import com.personal.toolkit.auth.security.JwtTokenService;
import com.personal.toolkit.auth.security.RestAuthenticationEntryPoint;
import com.personal.toolkit.common.exception.GlobalExceptionHandler;
import com.personal.toolkit.todo.dto.TodoSubItemRequest;
import com.personal.toolkit.todo.dto.TodoSubItemResponse;
import com.personal.toolkit.todo.dto.TodoSubItemSummaryResponse;
import com.personal.toolkit.todo.service.TodoSubItemService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
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
 * 验证 TodoSubItemController 在开启认证后暴露的嵌套 checklist 接口契约与校验行为。
 */
@WebMvcTest(TodoSubItemController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class
})
@WithMockUser(username = "alice")
class TodoSubItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TodoSubItemService todoSubItemService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    /**
     * 未认证访问 checklist 接口时应返回统一 401 错误响应。
     */
    @Test
    @WithAnonymousUser
    void findByTodoIdShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/todos/1/sub-items"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Authentication required"));
    }

    /**
     * 子任务列表接口应返回统一成功响应和数组数据。
     */
    @Test
    void findByTodoIdShouldReturnSubItemList() throws Exception {
        when(todoSubItemService.findByTodoId(1L)).thenReturn(List.of(createResponse(5L, 1L, "Write tests", "PENDING", 0)));

        mockMvc.perform(get("/api/todos/1/sub-items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Todo sub items fetched successfully"))
                .andExpect(jsonPath("$.data[0].id").value(5L))
                .andExpect(jsonPath("$.data[0].title").value("Write tests"));
    }

    /**
     * 创建接口成功时应返回 201 和新建结果。
     */
    @Test
    void createShouldReturnCreatedSubItem() throws Exception {
        TodoSubItemRequest request = new TodoSubItemRequest();
        request.setTitle("Create checklist item");

        when(todoSubItemService.create(eq(1L), any(TodoSubItemRequest.class)))
                .thenReturn(createResponse(8L, 1L, "Create checklist item", "PENDING", 0));

        mockMvc.perform(post("/api/todos/1/sub-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Todo sub item created successfully"))
                .andExpect(jsonPath("$.data.id").value(8L));
    }

    /**
     * 更新接口应返回更新后的子任务契约。
     */
    @Test
    void updateShouldReturnUpdatedSubItem() throws Exception {
        TodoSubItemRequest request = new TodoSubItemRequest();
        request.setTitle("Updated item");
        request.setStatus("DONE");

        when(todoSubItemService.update(eq(1L), eq(9L), any(TodoSubItemRequest.class)))
                .thenReturn(createResponse(9L, 1L, "Updated item", "DONE", 2));

        mockMvc.perform(put("/api/todos/1/sub-items/9")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Todo sub item updated successfully"))
                .andExpect(jsonPath("$.data.status").value("DONE"));
    }

    /**
     * 删除接口应返回无 data 的统一成功响应。
     */
    @Test
    void deleteShouldReturnSuccessEnvelope() throws Exception {
        doNothing().when(todoSubItemService).delete(1L, 10L);

        mockMvc.perform(delete("/api/todos/1/sub-items/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Todo sub item deleted successfully"));
    }

    /**
     * 摘要接口应返回 checklist 聚合进度数据。
     */
    @Test
    void getSummaryShouldReturnProgressSummary() throws Exception {
        when(todoSubItemService.getSummary(1L)).thenReturn(new TodoSubItemSummaryResponse(4, 2, 50));

        mockMvc.perform(get("/api/todos/1/sub-items/summary"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Todo sub item summary fetched successfully"))
                .andExpect(jsonPath("$.data.totalCount").value(4))
                .andExpect(jsonPath("$.data.completedCount").value(2))
                .andExpect(jsonPath("$.data.progressPercent").value(50));
    }

    /**
     * 请求体校验失败时应返回字段级错误结构。
     */
    @Test
    void createShouldReturnValidationError() throws Exception {
        TodoSubItemRequest request = new TodoSubItemRequest();
        request.setStatus("PENDING");

        mockMvc.perform(post("/api/todos/1/sub-items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.validation.title[0]").exists());
    }

    private TodoSubItemResponse createResponse(Long id, Long todoId, String title, String status, Integer sortOrder) {
        TodoSubItemResponse response = new TodoSubItemResponse();
        response.setId(id);
        response.setTodoId(todoId);
        response.setTitle(title);
        response.setStatus(status);
        response.setSortOrder(sortOrder);
        response.setCreateTime(LocalDateTime.now());
        response.setUpdateTime(LocalDateTime.now());
        return response;
    }
}
