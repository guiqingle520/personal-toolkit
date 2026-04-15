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

    @NotBlank(message = "captchaId must not be blank")
    @Size(max = 64, message = "captchaId must not exceed 64 characters")
    private String captchaId;

    @NotBlank(message = "captchaCode must not be blank")
    @Size(max = 16, message = "captchaCode must not exceed 16 characters")
    private String captchaCode;

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

    /**
     * 返回验证码标识。
     *
     * @return 验证码标识
     */
    public String getCaptchaId() {
        return captchaId;
    }

    /**
     * 设置验证码标识。
     *
     * @param captchaId 验证码标识
     */
    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    /**
     * 返回验证码答案。
     *
     * @return 验证码答案
     */
    public String getCaptchaCode() {
        return captchaCode;
    }

    /**
     * 设置验证码答案。
     *
     * @param captchaCode 验证码答案
     */
    public void setCaptchaCode(String captchaCode) {
        this.captchaCode = captchaCode;
    }
}
