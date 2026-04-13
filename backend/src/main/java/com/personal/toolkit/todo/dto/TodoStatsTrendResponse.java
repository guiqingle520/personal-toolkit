package com.personal.toolkit.todo.dto;

import java.util.List;

/**
 * 描述 Todo 统计趋势面板需要的时间范围与每日完成数量序列。
 */
public class TodoStatsTrendResponse {

    private final String range;
    private final List<TodoStatsTrendItemResponse> items;

    public TodoStatsTrendResponse(String range, List<TodoStatsTrendItemResponse> items) {
        this.range = range;
        this.items = items;
    }

    /**
     * 返回当前趋势统计的时间范围标识。
     *
     * @return 时间范围，如 7d
     */
    public String getRange() {
        return range;
    }

    /**
     * 返回趋势展示所需的每日数据项。
     *
     * @return 趋势数据列表
     */
    public List<TodoStatsTrendItemResponse> getItems() {
        return items;
    }
}
