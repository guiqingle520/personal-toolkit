package com.personal.toolkit.auth.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * 描述运行时安全策略更新请求，当前仅开放访问令牌 TTL 与密码过期配置。
 */
public class SecurityPolicyUpdateRequest {

    @Positive(message = "accessTokenTtlSeconds must be greater than 0")
    private Long accessTokenTtlSeconds;

    @NotNull(message = "passwordExpiryEnabled must not be null")
    private Boolean passwordExpiryEnabled;

    @Positive(message = "passwordExpiryDays must be greater than 0")
    private Integer passwordExpiryDays;

    public Long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public void setAccessTokenTtlSeconds(Long accessTokenTtlSeconds) {
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public Boolean getPasswordExpiryEnabled() {
        return passwordExpiryEnabled;
    }

    public void setPasswordExpiryEnabled(Boolean passwordExpiryEnabled) {
        this.passwordExpiryEnabled = passwordExpiryEnabled;
    }

    public Integer getPasswordExpiryDays() {
        return passwordExpiryDays;
    }

    public void setPasswordExpiryDays(Integer passwordExpiryDays) {
        this.passwordExpiryDays = passwordExpiryDays;
    }
}
