package com.personal.toolkit.todo.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.toolkit.auth.security.CurrentUserProvider;
import com.personal.toolkit.todo.dto.TodoSavedViewRequest;
import com.personal.toolkit.todo.entity.TodoSavedView;
import com.personal.toolkit.todo.repository.TodoSavedViewRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TodoSavedViewServiceTest {

    private static final Long USER_ID = 101L;

    @Mock
    private TodoSavedViewRepository todoSavedViewRepository;

    @Mock
    private CurrentUserProvider currentUserProvider;

    private TodoSavedViewService todoSavedViewService;

    @BeforeEach
    void setUp() {
        todoSavedViewService = new TodoSavedViewService(todoSavedViewRepository, currentUserProvider, new ObjectMapper());
    }

    @Test
    void createSavedViewShouldPersistWhitelistedFilters() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(todoSavedViewRepository.existsByUserIdAndNameIgnoreCase(USER_ID, "Ops Focus")).thenReturn(false);
        when(todoSavedViewRepository.save(any(TodoSavedView.class))).thenAnswer(invocation -> {
            TodoSavedView savedView = invocation.getArgument(0);
            savedView.setId(1L);
            return savedView;
        });

        TodoSavedViewRequest request = new TodoSavedViewRequest();
        request.setName(" Ops Focus ");
        request.setFilters(Map.of("status", "PENDING", "keyword", "release", "page", 3));

        var response = todoSavedViewService.createSavedView(request);

        assertEquals("Ops Focus", response.getName());
        assertEquals(Map.of("status", "PENDING", "keyword", "release"), response.getFilters());
    }

    @Test
    void createSavedViewShouldRejectDuplicateName() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(todoSavedViewRepository.existsByUserIdAndNameIgnoreCase(USER_ID, "Ops Focus")).thenReturn(true);

        TodoSavedViewRequest request = new TodoSavedViewRequest();
        request.setName("Ops Focus");

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () -> todoSavedViewService.createSavedView(request));

        assertEquals(400, exception.getStatusCode().value());
        verify(todoSavedViewRepository, never()).save(any(TodoSavedView.class));
    }

    @Test
    void setDefaultSavedViewShouldClearPreviousDefault() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        TodoSavedView previousDefault = createSavedView(1L, "Default", true, "{\"status\":\"PENDING\"}");
        TodoSavedView nextDefault = createSavedView(2L, "Later", false, "{\"status\":\"DONE\"}");
        when(todoSavedViewRepository.findByIdAndUserId(2L, USER_ID)).thenReturn(Optional.of(nextDefault));
        when(todoSavedViewRepository.findByUserIdAndDefaultViewTrue(USER_ID)).thenReturn(Optional.of(previousDefault));
        when(todoSavedViewRepository.save(any(TodoSavedView.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var response = todoSavedViewService.setDefaultSavedView(2L);

        assertTrue(response.isDefault());
        assertEquals(false, previousDefault.isDefaultView());
    }

    @Test
    void listSavedViewsShouldReturnParsedFilters() {
        when(currentUserProvider.getCurrentUserId()).thenReturn(USER_ID);
        when(todoSavedViewRepository.findByUserIdOrderByCreateTimeAsc(USER_ID)).thenReturn(List.of(createSavedView(1L, "Ops Focus", false, "{\"status\":\"PENDING\"}")));

        var result = todoSavedViewService.listSavedViews();

        assertEquals(1, result.size());
        assertEquals("PENDING", result.get(0).getFilters().get("status"));
    }

    private TodoSavedView createSavedView(Long id, String name, boolean isDefault, String filtersJson) {
        TodoSavedView todoSavedView = new TodoSavedView();
        todoSavedView.setId(id);
        todoSavedView.setUserId(USER_ID);
        todoSavedView.setName(name);
        todoSavedView.setDefaultView(isDefault);
        todoSavedView.setFiltersJson(filtersJson);
        return todoSavedView;
    }
}
