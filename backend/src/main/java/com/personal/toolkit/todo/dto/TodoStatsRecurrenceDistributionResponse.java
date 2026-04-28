package com.personal.toolkit.todo.dto;

import java.util.List;

/**
 * 描述活动任务按重复类型分布的统计结果。
 */
public class TodoStatsRecurrenceDistributionResponse {

    private final List<TodoStatsRecurrenceDistributionItemResponse> items;
    private final long totalActive;

    public TodoStatsRecurrenceDistributionResponse(List<TodoStatsRecurrenceDistributionItemResponse> items, long totalActive) {
        this.items = items;
        this.totalActive = totalActive;
    }

    public List<TodoStatsRecurrenceDistributionItemResponse> getItems() {
        return items;
    }

    public long getTotalActive() {
        return totalActive;
    }
}
