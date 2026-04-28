package com.personal.toolkit.todo.dto;

import java.util.List;

/**
 * 描述活动任务按优先级分布的统计结果。
 */
public class TodoStatsPriorityDistributionResponse {

    private final List<TodoStatsPriorityDistributionItemResponse> items;
    private final long totalActive;

    public TodoStatsPriorityDistributionResponse(List<TodoStatsPriorityDistributionItemResponse> items, long totalActive) {
        this.items = items;
        this.totalActive = totalActive;
    }

    public List<TodoStatsPriorityDistributionItemResponse> getItems() {
        return items;
    }

    public long getTotalActive() {
        return totalActive;
    }
}
