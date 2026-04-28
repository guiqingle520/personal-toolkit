package com.personal.toolkit.todo.dto;

/**
 * 描述统计趋势中的单日完成数量，用于前端绘制最近 7 天趋势。
 */
public class TodoStatsTrendItemResponse {

    private final String date;
    private final long createdCount;
    private final long completedCount;

    public TodoStatsTrendItemResponse(String date, long completedCount) {
        this(date, 0L, completedCount);
    }

    public TodoStatsTrendItemResponse(String date, long createdCount, long completedCount) {
        this.date = date;
        this.createdCount = createdCount;
        this.completedCount = completedCount;
    }

    /**
     * 返回趋势项对应的日期字符串，格式为 yyyy-MM-dd。
     *
     * @return 日期字符串
     */
    public String getDate() {
        return date;
    }

    /**
     * 返回该日期下的新建任务数量。
     *
     * @return 新建任务数
     */
    public long getCreatedCount() {
        return createdCount;
    }

    /**
     * 返回该日期下的完成任务数量。
     *
     * @return 完成任务数
     */
    public long getCompletedCount() {
        return completedCount;
    }
}
