package com.personal.toolkit.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 描述用户登录时提交的用户名和密码信息。
 */
public class AuthLoginRequest {

    @NotBlank(message = "username must not be blank")
    @Size(max = 100, message = "username must not exceed 100 characters")
    private String username;

    @NotBlank(message = "password must not be blank")
    @Size(max = 100, message = "password must not exceed 100 characters")
    private String password;

    /**
     * 返回登录用户名。
     *
     * @return 登录用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置登录用户名。
     *
     * @param username 登录用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 返回登录密码明文。
     *
     * @return 登录密码明文
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置登录密码明文。
     *
     * @param password 登录密码明文
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
