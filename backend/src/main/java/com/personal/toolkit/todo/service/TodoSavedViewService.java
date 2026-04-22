package com.personal.toolkit.todo.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.personal.toolkit.auth.security.CurrentUserProvider;
import com.personal.toolkit.todo.dto.TodoSavedViewRequest;
import com.personal.toolkit.todo.dto.TodoSavedViewResponse;
import com.personal.toolkit.todo.entity.TodoSavedView;
import com.personal.toolkit.todo.repository.TodoSavedViewRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * 处理 Todo 已保存筛选视图的 CRUD 和默认视图管理。
 */
@Service
public class TodoSavedViewService {

    private static final Set<String> ALLOWED_FILTER_KEYS = Set.of(
            "status",
            "priority",
            "category",
            "keyword",
            "tag",
            "recurrenceType",
            "timePreset",
            "dueDateFrom",
            "dueDateTo",
            "remindDateFrom",
            "remindDateTo",
            "sortBy",
            "sortDir"
    );

    private final TodoSavedViewRepository todoSavedViewRepository;
    private final CurrentUserProvider currentUserProvider;
    private final ObjectMapper objectMapper;

    public TodoSavedViewService(TodoSavedViewRepository todoSavedViewRepository,
                                CurrentUserProvider currentUserProvider,
                                ObjectMapper objectMapper) {
        this.todoSavedViewRepository = todoSavedViewRepository;
        this.currentUserProvider = currentUserProvider;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<TodoSavedViewResponse> listSavedViews() {
        return todoSavedViewRepository.findByUserIdOrderByCreateTimeAsc(currentUserProvider.getCurrentUserId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TodoSavedViewResponse createSavedView(TodoSavedViewRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        String normalizedName = normalizeName(request.getName());
        if (todoSavedViewRepository.existsByUserIdAndNameIgnoreCase(userId, normalizedName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saved view name already exists: " + normalizedName);
        }

        TodoSavedView todoSavedView = new TodoSavedView();
        todoSavedView.setUserId(userId);
        todoSavedView.setName(normalizedName);
        todoSavedView.setDefaultView(request.isDefault());
        todoSavedView.setFiltersJson(serializeFilters(normalizeFilters(request.getFilters())));
        if (request.isDefault()) {
            clearDefaultView(userId);
        }
        return toResponse(todoSavedViewRepository.save(todoSavedView));
    }

    @Transactional
    public TodoSavedViewResponse updateSavedView(Long id, TodoSavedViewRequest request) {
        Long userId = currentUserProvider.getCurrentUserId();
        TodoSavedView todoSavedView = getExistingSavedView(id, userId);
        String normalizedName = normalizeName(request.getName());
        if (todoSavedViewRepository.existsByUserIdAndNameIgnoreCaseAndIdNot(userId, normalizedName, id)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Saved view name already exists: " + normalizedName);
        }

        todoSavedView.setName(normalizedName);
        todoSavedView.setFiltersJson(serializeFilters(normalizeFilters(request.getFilters())));
        if (request.isDefault()) {
            clearDefaultView(userId);
            todoSavedView.setDefaultView(true);
        } else if (todoSavedView.isDefaultView()) {
            todoSavedView.setDefaultView(false);
        }
        return toResponse(todoSavedViewRepository.save(todoSavedView));
    }

    @Transactional
    public void deleteSavedView(Long id) {
        todoSavedViewRepository.delete(getExistingSavedView(id, currentUserProvider.getCurrentUserId()));
    }

    @Transactional
    public TodoSavedViewResponse setDefaultSavedView(Long id) {
        Long userId = currentUserProvider.getCurrentUserId();
        TodoSavedView todoSavedView = getExistingSavedView(id, userId);
        clearDefaultView(userId);
        todoSavedView.setDefaultView(true);
        return toResponse(todoSavedViewRepository.save(todoSavedView));
    }

    private TodoSavedView getExistingSavedView(Long id, Long userId) {
        return todoSavedViewRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Todo saved view not found: " + id));
    }

    private void clearDefaultView(Long userId) {
        todoSavedViewRepository.findByUserIdAndDefaultViewTrue(userId).ifPresent(savedView -> {
            savedView.setDefaultView(false);
            todoSavedViewRepository.save(savedView);
        });
    }

    private String normalizeName(String name) {
        String normalizedName = name == null ? "" : name.trim();
        if (normalizedName.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name must not be blank");
        }
        if (normalizedName.length() > 100) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "name must not exceed 100 characters");
        }
        return normalizedName;
    }

    private Map<String, Object> normalizeFilters(Map<String, Object> filters) {
        Map<String, Object> normalizedFilters = new LinkedHashMap<>();
        if (filters == null) {
            return normalizedFilters;
        }

        filters.forEach((key, value) -> {
            if (!ALLOWED_FILTER_KEYS.contains(key) || value == null) {
                return;
            }
            String normalizedValue = String.valueOf(value).trim();
            if (!normalizedValue.isEmpty()) {
                normalizedFilters.put(key, normalizedValue);
            }
        });
        return normalizedFilters;
    }

    private String serializeFilters(Map<String, Object> filters) {
        try {
            return objectMapper.writeValueAsString(filters);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize saved view filters");
        }
    }

    private Map<String, Object> deserializeFilters(String filtersJson) {
        try {
            return objectMapper.readValue(filtersJson, new TypeReference<>() {});
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to deserialize saved view filters");
        }
    }

    private TodoSavedViewResponse toResponse(TodoSavedView todoSavedView) {
        return new TodoSavedViewResponse(
                todoSavedView.getId(),
                todoSavedView.getName(),
                todoSavedView.isDefaultView(),
                deserializeFilters(todoSavedView.getFiltersJson()),
                todoSavedView.getCreateTime(),
                todoSavedView.getUpdateTime()
        );
    }
}
