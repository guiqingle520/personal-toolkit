package com.personal.toolkit.todo.controller;

import com.personal.toolkit.common.api.ApiResponse;
import com.personal.toolkit.todo.dto.TodoSavedViewRequest;
import com.personal.toolkit.todo.dto.TodoSavedViewResponse;
import com.personal.toolkit.todo.service.TodoSavedViewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 暴露 Todo Saved Views 的 REST 接口。
 */
@RestController
@RequestMapping("/api/todo-saved-views")
public class TodoSavedViewController {

    private final TodoSavedViewService todoSavedViewService;

    public TodoSavedViewController(TodoSavedViewService todoSavedViewService) {
        this.todoSavedViewService = todoSavedViewService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<TodoSavedViewResponse>>> listSavedViews() {
        return ResponseEntity.ok(ApiResponse.success("Todo saved views fetched successfully", todoSavedViewService.listSavedViews()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<TodoSavedViewResponse>> createSavedView(@Valid @RequestBody TodoSavedViewRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Todo saved view created successfully", todoSavedViewService.createSavedView(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<TodoSavedViewResponse>> updateSavedView(@PathVariable Long id,
                                                                              @Valid @RequestBody TodoSavedViewRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Todo saved view updated successfully", todoSavedViewService.updateSavedView(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteSavedView(@PathVariable Long id) {
        todoSavedViewService.deleteSavedView(id);
        return ResponseEntity.ok(ApiResponse.success("Todo saved view deleted successfully"));
    }

    @PostMapping("/{id}/default")
    public ResponseEntity<ApiResponse<TodoSavedViewResponse>> setDefaultSavedView(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Todo saved view default updated successfully", todoSavedViewService.setDefaultSavedView(id)));
    }
}
