package com.personal.toolkit.todo.dto;

/**
 * 描述单个重复类型对应的活动任务数量。
 */
public class TodoStatsRecurrenceDistributionItemResponse {

    private final String recurrenceType;
    private final long count;

    public TodoStatsRecurrenceDistributionItemResponse(String recurrenceType, long count) {
        this.recurrenceType = recurrenceType;
        this.count = count;
    }

    public String getRecurrenceType() {
        return recurrenceType;
    }

    public long getCount() {
        return count;
    }
}
