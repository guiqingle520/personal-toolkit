package com.personal.toolkit.todo.service;

/**
 * 统一维护 Todo 模块的 Redis 缓存键格式，避免跨服务手写字符串导致失配。
 */
public final class TodoCacheKeys {

    private TodoCacheKeys() {
    }

    /**
     * 生成单个待办事项详情缓存键。
     *
     * @param userId 待办事项所属用户主键
     * @param todoId 待办事项主键
     * @return Redis 中使用的详情缓存键
     */
    public static String todoItem(Long userId, Long todoId) {
        return "todo:item:user:" + userId + ":" + todoId;
    }
}
