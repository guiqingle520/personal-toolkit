package com.personal.toolkit.todo.dto;

/**
 * 描述提醒模块更完整的摘要统计信息。
 */
public class TodoReminderSummaryResponse {

    private final long unreadCount;
    private final long readTodayCount;
    private final long scheduledCount;
    private final long overdueReminderCount;

    public TodoReminderSummaryResponse(long unreadCount,
                                       long readTodayCount,
                                       long scheduledCount,
                                       long overdueReminderCount) {
        this.unreadCount = unreadCount;
        this.readTodayCount = readTodayCount;
        this.scheduledCount = scheduledCount;
        this.overdueReminderCount = overdueReminderCount;
    }

    public long getUnreadCount() {
        return unreadCount;
    }

    public long getReadTodayCount() {
        return readTodayCount;
    }

    public long getScheduledCount() {
        return scheduledCount;
    }

    public long getOverdueReminderCount() {
        return overdueReminderCount;
    }
}
