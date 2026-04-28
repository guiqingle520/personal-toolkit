package com.personal.toolkit.todo.dto;

import java.util.List;

/**
 * 描述活动待办的老化分布统计结果。
 */
public class TodoStatsAgingResponse {

    private final List<TodoStatsAgingBucketItemResponse> buckets;
    private final long totalPending;

    public TodoStatsAgingResponse(List<TodoStatsAgingBucketItemResponse> buckets, long totalPending) {
        this.buckets = buckets;
        this.totalPending = totalPending;
    }

    public List<TodoStatsAgingBucketItemResponse> getBuckets() {
        return buckets;
    }

    public long getTotalPending() {
        return totalPending;
    }
}
