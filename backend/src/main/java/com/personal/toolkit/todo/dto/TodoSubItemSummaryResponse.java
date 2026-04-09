package com.personal.toolkit.todo.dto;

/**
 * 描述主任务对应子任务的聚合进度摘要。
 */
public class TodoSubItemSummaryResponse {

    private final Integer totalCount;
    private final Integer completedCount;
    private final Integer progressPercent;

    public TodoSubItemSummaryResponse(Integer totalCount, Integer completedCount, Integer progressPercent) {
        this.totalCount = totalCount;
        this.completedCount = completedCount;
        this.progressPercent = progressPercent;
    }

    /**
     * 返回子任务总数。
     *
     * @return 子任务总数
     */
    public Integer getTotalCount() {
        return totalCount;
    }

    /**
     * 返回已完成子任务数。
     *
     * @return 已完成子任务数
     */
    public Integer getCompletedCount() {
        return completedCount;
    }

    /**
     * 返回完成进度百分比。
     *
     * @return 完成进度百分比
     */
    public Integer getProgressPercent() {
        return progressPercent;
    }
}
