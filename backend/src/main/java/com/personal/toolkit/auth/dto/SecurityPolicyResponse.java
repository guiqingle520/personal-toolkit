package com.personal.toolkit.auth.dto;

/**
 * 表示返回给前端的运行时安全策略配置快照，包含数据库原值与当前生效 TTL。
 */
public class SecurityPolicyResponse {

    private Long accessTokenTtlSeconds;
    private long effectiveAccessTokenTtlSeconds;
    private boolean passwordExpiryEnabled;
    private Integer passwordExpiryDays;

    public SecurityPolicyResponse() {
    }

    public SecurityPolicyResponse(Long accessTokenTtlSeconds,
                                  long effectiveAccessTokenTtlSeconds,
                                  boolean passwordExpiryEnabled,
                                  Integer passwordExpiryDays) {
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
        this.effectiveAccessTokenTtlSeconds = effectiveAccessTokenTtlSeconds;
        this.passwordExpiryEnabled = passwordExpiryEnabled;
        this.passwordExpiryDays = passwordExpiryDays;
    }

    public Long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public void setAccessTokenTtlSeconds(Long accessTokenTtlSeconds) {
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
    }

    public long getEffectiveAccessTokenTtlSeconds() {
        return effectiveAccessTokenTtlSeconds;
    }

    public void setEffectiveAccessTokenTtlSeconds(long effectiveAccessTokenTtlSeconds) {
        this.effectiveAccessTokenTtlSeconds = effectiveAccessTokenTtlSeconds;
    }

    public boolean isPasswordExpiryEnabled() {
        return passwordExpiryEnabled;
    }

    public void setPasswordExpiryEnabled(boolean passwordExpiryEnabled) {
        this.passwordExpiryEnabled = passwordExpiryEnabled;
    }

    public Integer getPasswordExpiryDays() {
        return passwordExpiryDays;
    }

    public void setPasswordExpiryDays(Integer passwordExpiryDays) {
        this.passwordExpiryDays = passwordExpiryDays;
    }
}
