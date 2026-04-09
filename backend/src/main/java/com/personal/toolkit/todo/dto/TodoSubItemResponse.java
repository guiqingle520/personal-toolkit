package com.personal.toolkit.todo.dto;

import java.time.LocalDateTime;

/**
 * 描述单条子任务对外返回的数据结构。
 */
public class TodoSubItemResponse {

    private Long id;
    private Long todoId;
    private String title;
    private String status;
    private Integer sortOrder;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    /**
     * 返回子任务主键。
     *
     * @return 子任务主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 设置子任务主键。
     *
     * @param id 子任务主键
     */
    public void setId(Long id) {
        this.id = id;
    }

    /**
     * 返回所属主任务主键。
     *
     * @return 主任务主键
     */
    public Long getTodoId() {
        return todoId;
    }

    /**
     * 设置所属主任务主键。
     *
     * @param todoId 主任务主键
     */
    public void setTodoId(Long todoId) {
        this.todoId = todoId;
    }

    /**
     * 返回子任务标题。
     *
     * @return 子任务标题
     */
    public String getTitle() {
        return title;
    }

    /**
     * 设置子任务标题。
     *
     * @param title 子任务标题
     */
    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * 返回子任务状态。
     *
     * @return 子任务状态
     */
    public String getStatus() {
        return status;
    }

    /**
     * 设置子任务状态。
     *
     * @param status 子任务状态
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * 返回子任务排序值。
     *
     * @return 子任务排序值
     */
    public Integer getSortOrder() {
        return sortOrder;
    }

    /**
     * 设置子任务排序值。
     *
     * @param sortOrder 子任务排序值
     */
    public void setSortOrder(Integer sortOrder) {
        this.sortOrder = sortOrder;
    }

    /**
     * 返回子任务创建时间。
     *
     * @return 创建时间
     */
    public LocalDateTime getCreateTime() {
        return createTime;
    }

    /**
     * 设置子任务创建时间。
     *
     * @param createTime 创建时间
     */
    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    /**
     * 返回子任务最后更新时间。
     *
     * @return 更新时间
     */
    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    /**
     * 设置子任务最后更新时间。
     *
     * @param updateTime 更新时间
     */
    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }
}
