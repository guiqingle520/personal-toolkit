package com.personal.toolkit.todo.dto;

/**
 * 描述提醒模块的汇总指标。
 */
public class TodoReminderStatsResponse {

    private final long unreadCount;

    public TodoReminderStatsResponse(long unreadCount) {
        this.unreadCount = unreadCount;
    }

    public long getUnreadCount() {
        return unreadCount;
    }
}
