package com.personal.toolkit.todo.controller;

import com.personal.toolkit.auth.config.SecurityConfig;
import com.personal.toolkit.auth.security.AppUserDetailsService;
import com.personal.toolkit.auth.security.JwtAuthenticationFilter;
import com.personal.toolkit.auth.security.JwtTokenService;
import com.personal.toolkit.auth.security.RestAuthenticationEntryPoint;
import com.personal.toolkit.common.exception.GlobalExceptionHandler;
import com.personal.toolkit.todo.dto.PageResponse;
import com.personal.toolkit.todo.dto.TodoReminderItemResponse;
import com.personal.toolkit.todo.dto.TodoReminderStatsResponse;
import com.personal.toolkit.todo.service.TodoReminderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TodoReminderController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class
})
@WithMockUser(username = "alice")
class TodoReminderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TodoReminderService todoReminderService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    @Test
    @WithAnonymousUser
    void listRemindersShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/todo-reminders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listRemindersShouldReturnPagedPayload() throws Exception {
        PageResponse<TodoReminderItemResponse> response = PageResponse.from(new PageImpl<>(
                List.of(new TodoReminderItemResponse(1L, 8L, "Review logs", "PENDING", "Work", LocalDateTime.of(2026, 4, 22, 21, 0), LocalDateTime.of(2026, 4, 22, 20, 30), "SENT", LocalDateTime.of(2026, 4, 22, 20, 30), null)),
                PageRequest.of(0, 10),
                1
        ));
        when(todoReminderService.queryReminderEvents(any())).thenReturn(response);

        mockMvc.perform(get("/api/todo-reminders").param("status", "SENT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Todo reminders fetched successfully"))
                .andExpect(jsonPath("$.data.content[0].todoTitle").value("Review logs"));
    }

    @Test
    void getReminderStatsShouldReturnStatsPayload() throws Exception {
        when(todoReminderService.getReminderStats()).thenReturn(new TodoReminderStatsResponse(4));

        mockMvc.perform(get("/api/todo-reminders/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.unreadCount").value(4));
    }

    @Test
    void markReminderAsReadShouldReturnSuccess() throws Exception {
        doNothing().when(todoReminderService).markReminderAsRead(5L);

        mockMvc.perform(post("/api/todo-reminders/5/read"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Todo reminder marked as read successfully"));
    }

    @Test
    void markAllRemindersAsReadShouldReturnSuccess() throws Exception {
        doNothing().when(todoReminderService).markAllRemindersAsRead();

        mockMvc.perform(post("/api/todo-reminders/read-all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Todo reminders marked as read successfully"));
    }
}
