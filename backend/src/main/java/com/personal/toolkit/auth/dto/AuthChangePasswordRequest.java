package com.personal.toolkit.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 描述已登录用户自助修改密码时提交的当前密码、新密码与确认密码。
 */
public class AuthChangePasswordRequest {

    @NotBlank(message = "currentPassword must not be blank")
    @Size(max = 100, message = "currentPassword must not exceed 100 characters")
    private String currentPassword;

    @NotBlank(message = "newPassword must not be blank")
    @Size(min = 8, max = 100, message = "newPassword must be between 8 and 100 characters")
    private String newPassword;

    @NotBlank(message = "confirmPassword must not be blank")
    @Size(min = 8, max = 100, message = "confirmPassword must be between 8 and 100 characters")
    private String confirmPassword;

    public String getCurrentPassword() {
        return currentPassword;
    }

    public void setCurrentPassword(String currentPassword) {
        this.currentPassword = currentPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public void setNewPassword(String newPassword) {
        this.newPassword = newPassword;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public void setConfirmPassword(String confirmPassword) {
        this.confirmPassword = confirmPassword;
    }
}
