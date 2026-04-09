package com.personal.toolkit.todo.dto;

import java.util.List;

/**
 * 描述前端筛选器需要的分类和标签候选项。
 */
public class TodoOptionResponse {

    private final List<String> categories;
    private final List<String> tags;

    public TodoOptionResponse(List<String> categories, List<String> tags) {
        this.categories = categories;
        this.tags = tags;
    }

    /**
     * 返回可用的分类选项列表。
     *
     * @return 分类选项列表
     */
    public List<String> getCategories() {
        return categories;
    }

    /**
     * 返回可用的标签选项列表。
     *
     * @return 标签选项列表
     */
    public List<String> getTags() {
        return tags;
    }
}
