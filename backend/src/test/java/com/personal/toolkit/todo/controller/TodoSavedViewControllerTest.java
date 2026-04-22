package com.personal.toolkit.todo.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.toolkit.auth.config.SecurityConfig;
import com.personal.toolkit.auth.security.AppUserDetailsService;
import com.personal.toolkit.auth.security.JwtAuthenticationFilter;
import com.personal.toolkit.auth.security.JwtTokenService;
import com.personal.toolkit.auth.security.RestAuthenticationEntryPoint;
import com.personal.toolkit.common.exception.GlobalExceptionHandler;
import com.personal.toolkit.todo.dto.TodoSavedViewRequest;
import com.personal.toolkit.todo.dto.TodoSavedViewResponse;
import com.personal.toolkit.todo.service.TodoSavedViewService;
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
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TodoSavedViewController.class)
@Import({
        GlobalExceptionHandler.class,
        SecurityConfig.class,
        JwtAuthenticationFilter.class,
        RestAuthenticationEntryPoint.class
})
@WithMockUser(username = "alice")
class TodoSavedViewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TodoSavedViewService todoSavedViewService;

    @MockBean
    private JwtTokenService jwtTokenService;

    @MockBean
    private AppUserDetailsService appUserDetailsService;

    @Test
    @WithAnonymousUser
    void listSavedViewsShouldRequireAuthentication() throws Exception {
        mockMvc.perform(get("/api/todo-saved-views"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void listSavedViewsShouldReturnPayload() throws Exception {
        when(todoSavedViewService.listSavedViews()).thenReturn(List.of(createSavedViewResponse(1L, "Ops Focus", false)));

        mockMvc.perform(get("/api/todo-saved-views"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("Ops Focus"));
    }

    @Test
    void createSavedViewShouldReturnCreated() throws Exception {
        TodoSavedViewRequest request = new TodoSavedViewRequest();
        request.setName("Ops Focus");
        request.setDefault(true);
        request.setFilters(Map.of("status", "PENDING"));
        when(todoSavedViewService.createSavedView(any(TodoSavedViewRequest.class))).thenReturn(createSavedViewResponse(1L, "Ops Focus", true));

                mockMvc.perform(post("/api/todo-saved-views")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.default").value(true));
    }

    @Test
    void updateSavedViewShouldReturnUpdatedPayload() throws Exception {
        TodoSavedViewRequest request = new TodoSavedViewRequest();
        request.setName("Ops Focus Updated");
        request.setFilters(Map.of("keyword", "release"));
        when(todoSavedViewService.updateSavedView(any(Long.class), any(TodoSavedViewRequest.class))).thenReturn(createSavedViewResponse(1L, "Ops Focus Updated", false));

        mockMvc.perform(put("/api/todo-saved-views/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("Ops Focus Updated"));
    }

    @Test
    void deleteSavedViewShouldReturnSuccess() throws Exception {
        doNothing().when(todoSavedViewService).deleteSavedView(1L);

        mockMvc.perform(delete("/api/todo-saved-views/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Todo saved view deleted successfully"));
    }

    @Test
    void setDefaultSavedViewShouldReturnPayload() throws Exception {
        when(todoSavedViewService.setDefaultSavedView(1L)).thenReturn(createSavedViewResponse(1L, "Ops Focus", true));

        mockMvc.perform(post("/api/todo-saved-views/1/default"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.default").value(true));
    }

    private TodoSavedViewResponse createSavedViewResponse(Long id, String name, boolean isDefault) {
        return new TodoSavedViewResponse(id, name, isDefault, Map.of("status", "PENDING"), LocalDateTime.now(), LocalDateTime.now());
    }
}
