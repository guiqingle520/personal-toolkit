package com.personal.toolkit.todo.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 描述待办事项批量操作时提交的主键集合。
 */
public class TodoBatchRequest {

    @NotEmpty(message = "ids must not be empty")
    private List<Long> ids;

    /**
     * 返回批量操作的待办事项主键集合。
     *
     * @return 待办事项主键列表
     */
    public List<Long> getIds() {
        return ids;
    }

    /**
     * 设置批量操作的待办事项主键集合。
     *
     * @param ids 待办事项主键列表
     */
    public void setIds(List<Long> ids) {
        this.ids = ids;
    }
}
