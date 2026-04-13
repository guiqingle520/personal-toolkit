package com.personal.toolkit.auth.security;

/**
 * 定义读取当前请求用户身份的统一入口，避免业务层直接依赖 Spring Security 上下文。
 */
public interface CurrentUserProvider {

    /**
     * 返回当前请求中的已登录用户信息，缺失时抛出鉴权失败异常。
     *
     * @return 当前已登录用户信息
     */
    AuthenticatedUser getCurrentUser();

    /**
     * 返回当前请求用户主键，供按用户隔离的查询和缓存键拼装复用。
     *
     * @return 当前登录用户主键
     */
    default Long getCurrentUserId() {
        return getCurrentUser().getId();
    }
}
