package com.personal.toolkit.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

/**
 * 描述新增或更新待办事项时由客户端提交的请求体结构。
 */
public class TodoItemRequest {

    @NotBlank(message = "title must not be blank")
    @Size(max = 200, message = "title must not exceed 200 characters")
    private String title;

    @NotBlank(message = "status must not be blank")
    @Size(max = 32, message = "status must not exceed 32 characters")
    private String status;

    @NotNull(message = "priority must not be null")
    private Integer priority;

    private LocalDateTime dueDate;

    @Size(max = 100, message = "category must not exceed 100 characters")
    private String category;

    @Size(max = 500, message = "tags must not exceed 500 characters")
    private String tags;

    @Size(max = 32, message = "recurrenceType must not exceed 32 characters")
    private String recurrenceType;

    private Integer recurrenceInterval;

    private LocalDateTime recurrenceEndTime;

    /**
     * 返回待办事项标题。
     *
     * @return 待办标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置待办事项标题。
     *
     * @param title 待办标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 返回待办事项状态。
     *
     * @return 待办状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置待办事项状态。
     *
     * @param status 待办状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 返回待办事项优先级。
     *
     * @return 优先级，1 最低，5 最高
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * 设置待办事项优先级。
     *
     * @param priority 优先级，1 最低，5 最高
     */
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * 返回待办事项截止时间。
     *
     * @return 截止时间
     */
    public LocalDateTime getDueDate() {
        return dueDate;
    }

    /**
     * 设置待办事项截止时间。
     *
     * @param dueDate 截止时间
     */
    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }

    /**
     * 返回待办事项分类。
     *
     * @return 分类名称
     */
    public String getCategory() {
        return category;
    }

    /**
     * 设置待办事项分类。
     *
     * @param category 分类名称
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 返回待办事项标签字符串。
     *
     * @return 逗号分隔的标签字符串
     */
    public String getTags() {
        return tags;
    }

    /**
     * 设置待办事项标签字符串。
     *
     * @param tags 逗号分隔的标签字符串
     */
    public void setTags(String tags) {
        this.tags = tags;
    }

    /**
     * 返回重复任务类型。
     *
     * @return 重复任务类型
     */
    public String getRecurrenceType() {
        return recurrenceType;
    }

    /**
     * 设置重复任务类型。
     *
     * @param recurrenceType 重复任务类型
     */
    public void setRecurrenceType(String recurrenceType) {
        this.recurrenceType = recurrenceType;
    }

    /**
     * 返回重复任务间隔。
     *
     * @return 重复任务间隔
     */
    public Integer getRecurrenceInterval() {
        return recurrenceInterval;
    }

    /**
     * 设置重复任务间隔。
     *
     * @param recurrenceInterval 重复任务间隔
     */
    public void setRecurrenceInterval(Integer recurrenceInterval) {
        this.recurrenceInterval = recurrenceInterval;
    }

    /**
     * 返回重复任务截止时间。
     *
     * @return 重复任务截止时间
     */
    public LocalDateTime getRecurrenceEndTime() {
        return recurrenceEndTime;
    }

    /**
     * 设置重复任务截止时间。
     *
     * @param recurrenceEndTime 重复任务截止时间
     */
    public void setRecurrenceEndTime(LocalDateTime recurrenceEndTime) {
        this.recurrenceEndTime = recurrenceEndTime;
    }
}
