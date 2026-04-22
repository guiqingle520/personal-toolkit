package com.personal.toolkit.todo.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.Map;

/**
 * 描述保存 Todo 筛选视图的请求体。
 */
public class TodoSavedViewRequest {

    @NotBlank(message = "name must not be blank")
    private String name;

    private boolean isDefault;

    private Map<String, Object> filters;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public void setDefault(boolean aDefault) {
        isDefault = aDefault;
    }

    public Map<String, Object> getFilters() {
        return filters;
    }

    public void setFilters(Map<String, Object> filters) {
        this.filters = filters;
    }
}
