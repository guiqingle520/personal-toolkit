package com.personal.toolkit.todo.dto;

import java.time.LocalDateTime;

/**
 * 描述提醒列表中的单条站内提醒展示数据。
 */
public class TodoReminderItemResponse {

    private final Long id;
    private final Long todoId;
    private final String todoTitle;
    private final String todoStatus;
    private final String category;
    private final LocalDateTime dueDate;
    private final LocalDateTime scheduledAt;
    private final String status;
    private final LocalDateTime sentAt;
    private final LocalDateTime readAt;

    public TodoReminderItemResponse(Long id,
                                    Long todoId,
                                    String todoTitle,
                                    String todoStatus,
                                    String category,
                                    LocalDateTime dueDate,
                                    LocalDateTime scheduledAt,
                                    String status,
                                    LocalDateTime sentAt,
                                    LocalDateTime readAt) {
        this.id = id;
        this.todoId = todoId;
        this.todoTitle = todoTitle;
        this.todoStatus = todoStatus;
        this.category = category;
        this.dueDate = dueDate;
        this.scheduledAt = scheduledAt;
        this.status = status;
        this.sentAt = sentAt;
        this.readAt = readAt;
    }

    public Long getId() {
        return id;
    }

    public Long getTodoId() {
        return todoId;
    }

    public String getTodoTitle() {
        return todoTitle;
    }

    public String getTodoStatus() {
        return todoStatus;
    }

    public String getCategory() {
        return category;
    }

    public LocalDateTime getDueDate() {
        return dueDate;
    }

    public LocalDateTime getScheduledAt() {
        return scheduledAt;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }
}
