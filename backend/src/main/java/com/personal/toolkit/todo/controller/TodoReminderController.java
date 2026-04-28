package com.personal.toolkit.todo.controller;

import com.personal.toolkit.common.api.ApiResponse;
import com.personal.toolkit.todo.dto.PageResponse;
import com.personal.toolkit.todo.dto.TodoReminderItemResponse;
import com.personal.toolkit.todo.dto.TodoReminderQueryRequest;
import com.personal.toolkit.todo.dto.TodoReminderSummaryResponse;
import com.personal.toolkit.todo.dto.TodoReminderStatsResponse;
import com.personal.toolkit.todo.service.TodoReminderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 暴露 Todo 站内提醒的 REST 接口。
 */
@RestController
@RequestMapping("/api/todo-reminders")
public class TodoReminderController {

    private final TodoReminderService todoReminderService;

    public TodoReminderController(TodoReminderService todoReminderService) {
        this.todoReminderService = todoReminderService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<TodoReminderItemResponse>>> listReminders(
            @ModelAttribute TodoReminderQueryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(
                "Todo reminders fetched successfully",
                todoReminderService.queryReminderEvents(request)
        ));
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<TodoReminderStatsResponse>> getReminderStats() {
        return ResponseEntity.ok(ApiResponse.success(
                "Todo reminder stats fetched successfully",
                todoReminderService.getReminderStats()
        ));
    }

    @GetMapping("/stats/summary")
    public ResponseEntity<ApiResponse<TodoReminderSummaryResponse>> getReminderSummary() {
        return ResponseEntity.ok(ApiResponse.success(
                "Todo reminder summary fetched successfully",
                todoReminderService.getReminderSummary()
        ));
    }

    @PostMapping("/{id}/read")
    public ResponseEntity<ApiResponse<Void>> markReminderAsRead(@PathVariable Long id) {
        todoReminderService.markReminderAsRead(id);
        return ResponseEntity.ok(ApiResponse.success("Todo reminder marked as read successfully"));
    }

    @PostMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllRemindersAsRead() {
        todoReminderService.markAllRemindersAsRead();
        return ResponseEntity.ok(ApiResponse.success("Todo reminders marked as read successfully"));
    }
}
