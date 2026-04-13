package com.personal.toolkit.auth.security;

/**
 * 表示当前请求中已经通过鉴权的用户标识信息，供业务层执行按用户隔离查询。
 */
public class AuthenticatedUser {

    private final Long id;
    private final String username;

    public AuthenticatedUser(Long id, String username) {
        this.id = id;
        this.username = username;
    }

    /**
     * 返回当前登录用户主键。
     *
     * @return 当前登录用户主键
     */
    public Long getId() {
        return id;
    }

    /**
     * 返回当前登录用户名。
     *
     * @return 当前登录用户名
     */
    public String getUsername() {
        return username;
    }
}
