package com.personal.toolkit.todo.dto;

import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 描述待办事项列表查询时可选的筛选条件。
 */
public class TodoQueryRequest {

    private String status;
    private Integer priority;
    private String category;
    private String keyword;
    private String tag;
    private String recurrenceType;
    private String timePreset;
    private Boolean includeDeleted;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateTo;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate remindDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate remindDateTo;

    /**
     * 返回待办状态筛选条件。
     *
     * @return 待办状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置待办状态筛选条件。
     *
     * @param status 待办状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 返回优先级筛选条件。
     *
     * @return 优先级
     */
    public Integer getPriority() {
        return priority;
    }

    /**
     * 设置优先级筛选条件。
     *
     * @param priority 优先级
     */
    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    /**
     * 返回分类筛选条件。
     *
     * @return 分类名称
     */
    public String getCategory() {
        return category;
    }

    /**
     * 设置分类筛选条件。
     *
     * @param category 分类名称
     */
    public void setCategory(String category) {
        this.category = category;
    }

    /**
     * 返回关键字筛选条件。
     *
     * @return 标题或分类关键字
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * 设置关键字筛选条件。
     *
     * @param keyword 标题或分类关键字
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    /**
     * 返回标签筛选条件。
     *
     * @return 单个标签关键字
     */
    public String getTag() {
        return tag;
    }

    /**
     * 设置标签筛选条件。
     *
     * @param tag 单个标签关键字
     */
    public void setTag(String tag) {
        this.tag = tag;
    }

    /**
     * 返回重复任务类型筛选条件。
     *
     * @return 重复任务类型
     */
    public String getRecurrenceType() {
        return recurrenceType;
    }

    /**
     * 设置重复任务类型筛选条件。
     *
     * @param recurrenceType 重复任务类型
     */
    public void setRecurrenceType(String recurrenceType) {
        this.recurrenceType = recurrenceType;
    }

    /**
     * 返回时间预设筛选条件。
     *
     * @return 时间预设
     */
    public String getTimePreset() {
        return timePreset;
    }

    /**
     * 设置时间预设筛选条件。
     *
     * @param timePreset 时间预设
     */
    public void setTimePreset(String timePreset) {
        this.timePreset = timePreset;
    }

    /**
     * 返回是否包含已删除数据。
     *
     * @return 是否包含已删除数据
     */
    public Boolean getIncludeDeleted() {
        return includeDeleted;
    }

    /**
     * 设置是否包含已删除数据。
     *
     * @param includeDeleted true 表示查询回收站数据
     */
    public void setIncludeDeleted(Boolean includeDeleted) {
        this.includeDeleted = includeDeleted;
    }

    /**
     * 返回截止时间下限。
     *
     * @return 截止时间开始值
     */
    public LocalDate getDueDateFrom() {
        return dueDateFrom;
    }

    /**
     * 设置截止时间下限。
     *
     * @param dueDateFrom 截止时间开始值
     */
    public void setDueDateFrom(LocalDate dueDateFrom) {
        this.dueDateFrom = dueDateFrom;
    }

    /**
     * 返回截止时间上限。
     *
     * @return 截止时间结束值
     */
    public LocalDate getDueDateTo() {
        return dueDateTo;
    }

    /**
     * 设置截止时间上限。
     *
     * @param dueDateTo 截止时间结束值
     */
    public void setDueDateTo(LocalDate dueDateTo) {
        this.dueDateTo = dueDateTo;
    }

    /**
     * 返回提醒时间下限。
     *
     * @return 提醒时间开始值
     */
    public LocalDate getRemindDateFrom() {
        return remindDateFrom;
    }

    /**
     * 设置提醒时间下限。
     *
     * @param remindDateFrom 提醒时间开始值
     */
    public void setRemindDateFrom(LocalDate remindDateFrom) {
        this.remindDateFrom = remindDateFrom;
    }

    /**
     * 返回提醒时间上限。
     *
     * @return 提醒时间结束值
     */
    public LocalDate getRemindDateTo() {
        return remindDateTo;
    }

    /**
     * 设置提醒时间上限。
     *
     * @param remindDateTo 提醒时间结束值
     */
    public void setRemindDateTo(LocalDate remindDateTo) {
        this.remindDateTo = remindDateTo;
    }
}
