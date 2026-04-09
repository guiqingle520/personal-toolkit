package com.personal.toolkit.todo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 描述新增或更新子任务时由客户端提交的请求体结构。
 */
public class TodoSubItemRequest {

    @NotBlank(message = "title must not be blank")
    @Size(max = 200, message = "title must not exceed 200 characters")
    private String title;

    @Size(max = 32, message = "status must not exceed 32 characters")
    private String status;

    private Integer sortOrder;

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
}
