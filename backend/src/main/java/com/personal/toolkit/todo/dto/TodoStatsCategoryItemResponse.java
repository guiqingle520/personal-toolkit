package com.personal.toolkit.todo.dto;

/**
 * 描述单个分类在统计面板中的活动数与完成数聚合结果。
 */
public class TodoStatsCategoryItemResponse {

    private final String category;
    private final long activeCount;
    private final long completedCount;

    public TodoStatsCategoryItemResponse(String category, long activeCount, long completedCount) {
        this.category = category;
        this.activeCount = activeCount;
        this.completedCount = completedCount;
    }

    /**
     * 返回分类名称；无分类任务会在服务层统一归并后写入占位名称。
     *
     * @return 分类名称
     */
    public String getCategory() {
        return category;
    }

    /**
     * 返回当前分类下未完成任务数量。
     *
     * @return 活动任务数
     */
    public long getActiveCount() {
        return activeCount;
    }

    /**
     * 返回当前分类下已完成任务数量。
     *
     * @return 已完成任务数
     */
    public long getCompletedCount() {
        return completedCount;
    }
}
