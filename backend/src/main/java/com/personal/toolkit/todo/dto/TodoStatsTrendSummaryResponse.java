package com.personal.toolkit.todo.dto;

import java.math.BigDecimal;

/**
 * 描述趋势统计顶部需要的汇总指标。
 */
public class TodoStatsTrendSummaryResponse {

    private final long totalCreated;
    private final long totalCompleted;
    private final BigDecimal completionRate;
    private final long netChange;

    public TodoStatsTrendSummaryResponse(long totalCreated,
                                         long totalCompleted,
                                         BigDecimal completionRate,
                                         long netChange) {
        this.totalCreated = totalCreated;
        this.totalCompleted = totalCompleted;
        this.completionRate = completionRate;
        this.netChange = netChange;
    }

    public long getTotalCreated() {
        return totalCreated;
    }

    public long getTotalCompleted() {
        return totalCompleted;
    }

    public BigDecimal getCompletionRate() {
        return completionRate;
    }

    public long getNetChange() {
        return netChange;
    }
}
