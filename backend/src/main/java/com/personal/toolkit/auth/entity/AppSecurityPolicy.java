package com.personal.toolkit.auth.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * 表示全局唯一的运行时安全策略，承载访问令牌 TTL 与密码过期策略等后端控制项。
 */
@Entity
@Table(name = "app_security_policy")
public class AppSecurityPolicy {

    public static final Long GLOBAL_POLICY_ID = 1L;

    @Id
    private Long id;

    @Column(name = "access_token_ttl_seconds")
    private Long accessTokenTtlSeconds;

    @Column(name = "password_expiry_enabled", nullable = false)
    private boolean passwordExpiryEnabled;

    @Column(name = "password_expiry_days")
    private Integer passwordExpiryDays;

    @Column(name = "create_time", nullable = false)
    private LocalDateTime createTime;

    @Column(name = "update_time", nullable = false)
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccessTokenTtlSeconds() {
        return accessTokenTtlSeconds;
    }

    public void setAccessTokenTtlSeconds(Long accessTokenTtlSeconds) {
        this.accessTokenTtlSeconds = accessTokenTtlSeconds;
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

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    @PrePersist
    public void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        this.createTime = now;
        this.updateTime = now;
    }

    @PreUpdate
    public void onUpdate() {
        this.updateTime = LocalDateTime.now();
    }
}
