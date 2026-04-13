package com.personal.toolkit.auth.dto;

/**
 * 表示登录或注册成功后返回给前端的 JWT 令牌与当前用户概要信息。
 */
public class AuthTokenResponse {

    private String token;
    private UserProfileResponse user;

    public AuthTokenResponse() {
    }

    public AuthTokenResponse(String token, UserProfileResponse user) {
        this.token = token;
        this.user = user;
    }

    /**
     * 返回 JWT 访问令牌。
     *
     * @return JWT 访问令牌
     */
    public String getToken() {
        return token;
    }

    /**
     * 设置 JWT 访问令牌。
     *
     * @param token JWT 访问令牌
     */
    public void setToken(String token) {
        this.token = token;
    }

    /**
     * 返回当前登录用户概要信息。
     *
     * @return 当前登录用户概要信息
     */
    public UserProfileResponse getUser() {
        return user;
    }

    /**
     * 设置当前登录用户概要信息。
     *
     * @param user 当前登录用户概要信息
     */
    public void setUser(UserProfileResponse user) {
        this.user = user;
    }
}
