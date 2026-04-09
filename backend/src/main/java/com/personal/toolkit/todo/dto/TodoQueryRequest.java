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
    private Boolean includeDeleted;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateFrom;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDateTo;

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
}
