package com.personal.toolkit.todo.dto;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 描述已保存的 Todo 筛选视图响应体。
 */
public class TodoSavedViewResponse {

    private final Long id;
    private final String name;
    private final boolean isDefault;
    private final Map<String, Object> filters;
    private final LocalDateTime createTime;
    private final LocalDateTime updateTime;

    public TodoSavedViewResponse(Long id,
                                 String name,
                                 boolean isDefault,
                                 Map<String, Object> filters,
                                 LocalDateTime createTime,
                                 LocalDateTime updateTime) {
        this.id = id;
        this.name = name;
        this.isDefault = isDefault;
        this.filters = filters;
        this.createTime = createTime;
        this.updateTime = updateTime;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }
}
