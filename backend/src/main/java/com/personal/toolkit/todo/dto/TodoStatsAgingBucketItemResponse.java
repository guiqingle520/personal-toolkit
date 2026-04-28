package com.personal.toolkit.todo.dto;

/**
 * 描述单个任务老化分桶的任务数量。
 */
public class TodoStatsAgingBucketItemResponse {

    private final String label;
    private final long count;

    public TodoStatsAgingBucketItemResponse(String label, long count) {
        this.label = label;
        this.count = count;
    }

    public String getLabel() {
        return label;
    }

    public long getCount() {
        return count;
    }
}
