package com.personal.toolkit.todo.dto;

/**
 * 描述活动任务在不同截止时间区间中的分布情况。
 */
public class TodoStatsDueBucketsResponse {

    private final long overdue;
    private final long dueToday;
    private final long dueIn3Days;
    private final long dueIn7Days;
    private final long noDueDate;
    private final long totalActive;

    public TodoStatsDueBucketsResponse(long overdue,
                                       long dueToday,
                                       long dueIn3Days,
                                       long dueIn7Days,
                                       long noDueDate,
                                       long totalActive) {
        this.overdue = overdue;
        this.dueToday = dueToday;
        this.dueIn3Days = dueIn3Days;
        this.dueIn7Days = dueIn7Days;
        this.noDueDate = noDueDate;
        this.totalActive = totalActive;
    }

    public long getOverdue() {
        return overdue;
    }

    public long getDueToday() {
        return dueToday;
    }

    public long getDueIn3Days() {
        return dueIn3Days;
    }

    public long getDueIn7Days() {
        return dueIn7Days;
    }

    public long getNoDueDate() {
        return noDueDate;
    }

    public long getTotalActive() {
        return totalActive;
    }
}
