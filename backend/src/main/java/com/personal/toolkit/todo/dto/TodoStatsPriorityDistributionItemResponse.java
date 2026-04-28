package com.personal.toolkit.todo.dto;

/**
 * 描述单个优先级下的活动任务数量。
 */
public class TodoStatsPriorityDistributionItemResponse {

    private final Integer priority;
    private final long count;

    public TodoStatsPriorityDistributionItemResponse(Integer priority, long count) {
        this.priority = priority;
        this.count = count;
    }

    public Integer getPriority() {
        return priority;
    }

    public long getCount() {
        return count;
    }
}
