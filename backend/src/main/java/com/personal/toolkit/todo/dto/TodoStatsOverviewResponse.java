package com.personal.toolkit.todo.dto;

/**
 * 描述 Todo 统计面板顶部概览卡片需要的汇总指标。
 */
public class TodoStatsOverviewResponse {

    private final long todayCompleted;
    private final long weekCompleted;
    private final long overdueCount;
    private final long activeCount;
    private final long upcomingReminderCount;
    private final long unreadReminderCount;

    public TodoStatsOverviewResponse(long todayCompleted,
                                     long weekCompleted,
                                     long overdueCount,
                                     long activeCount,
                                     long upcomingReminderCount,
                                     long unreadReminderCount) {
        this.todayCompleted = todayCompleted;
        this.weekCompleted = weekCompleted;
        this.overdueCount = overdueCount;
        this.activeCount = activeCount;
        this.upcomingReminderCount = upcomingReminderCount;
        this.unreadReminderCount = unreadReminderCount;
    }

    /**
     * 返回今日完成任务数量。
     *
     * @return 今日完成数
     */
    public long getTodayCompleted() {
        return todayCompleted;
    }

    /**
     * 返回当前自然周内完成的任务数量。
     *
     * @return 本周完成数
     */
    public long getWeekCompleted() {
        return weekCompleted;
    }

    /**
     * 返回当前处于逾期状态的未完成任务数量。
     *
     * @return 逾期任务数
     */
    public long getOverdueCount() {
        return overdueCount;
    }

    /**
     * 返回当前活动任务数量。
     *
     * @return 活动任务数
     */
    public long getActiveCount() {
        return activeCount;
    }

    /**
     * 返回未来 24 小时内即将触发的提醒数量。
     *
     * @return 即将提醒数量
     */
    public long getUpcomingReminderCount() {
        return upcomingReminderCount;
    }

    /**
     * 返回当前未读的站内提醒数量。
     *
     * @return 未读提醒数量
     */
    public long getUnreadReminderCount() {
        return unreadReminderCount;
    }
}
