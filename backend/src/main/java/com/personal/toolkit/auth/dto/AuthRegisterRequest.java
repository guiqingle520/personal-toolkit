package com.personal.toolkit.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 描述用户注册时提交的用户名、邮箱和密码信息。
 */
public class AuthRegisterRequest {

    @NotBlank(message = "username must not be blank")
    @Size(max = 100, message = "username must not exceed 100 characters")
    private String username;

    @NotBlank(message = "email must not be blank")
    @Email(message = "email must be a valid email address")
    @Size(max = 255, message = "email must not exceed 255 characters")
    private String email;

    @NotBlank(message = "password must not be blank")
    @Size(min = 8, max = 100, message = "password must be between 8 and 100 characters")
    private String password;

    /**
     * 返回注册用户名。
     *
     * @return 注册用户名
     */
    public String getUsername() {
        return username;
    }

    /**
     * 设置注册用户名。
     *
     * @param username 注册用户名
     */
    public void setUsername(String username) {
        this.username = username;
    }

    /**
     * 返回注册邮箱。
     *
     * @return 注册邮箱
     */
    public String getEmail() {
        return email;
    }

    /**
     * 设置注册邮箱。
     *
     * @param email 注册邮箱
     */
    public void setEmail(String email) {
        this.email = email;
    }

    /**
     * 返回注册密码明文。
     *
     * @return 注册密码明文
     */
    public String getPassword() {
        return password;
    }

    /**
     * 设置注册密码明文。
     *
     * @param password 注册密码明文
     */
    public void setPassword(String password) {
        this.password = password;
    }
}
