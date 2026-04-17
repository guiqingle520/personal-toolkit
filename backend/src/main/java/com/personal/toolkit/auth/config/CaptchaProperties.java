package com.personal.toolkit.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 聚合登录验证码与频控策略配置，便于不同环境按需调整安全阈值。
 */
@ConfigurationProperties(prefix = "app.auth.captcha")
public class CaptchaProperties {

    private boolean enabled = true;
    private boolean adaptive = false;
    private int adaptiveTriggerThreshold = 2;
    private int length = 5;
    private int maxAttempts = 3;
    private int issueThreshold = 20;
    private int loginFailThreshold = 5;
    private Duration ttl = Duration.ofSeconds(120);
    private Duration issueWindow = Duration.ofMinutes(1);
    private Duration loginFailWindow = Duration.ofMinutes(15);

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAdaptive() {
        return adaptive;
    }

    public void setAdaptive(boolean adaptive) {
        this.adaptive = adaptive;
    }

    public int getLength() {
        return length;
    }

    public int getAdaptiveTriggerThreshold() {
        return adaptiveTriggerThreshold;
    }

    public void setAdaptiveTriggerThreshold(int adaptiveTriggerThreshold) {
        this.adaptiveTriggerThreshold = adaptiveTriggerThreshold;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getIssueThreshold() {
        return issueThreshold;
    }

    public void setIssueThreshold(int issueThreshold) {
        this.issueThreshold = issueThreshold;
    }

    public int getLoginFailThreshold() {
        return loginFailThreshold;
    }

    public void setLoginFailThreshold(int loginFailThreshold) {
        this.loginFailThreshold = loginFailThreshold;
    }

    public Duration getTtl() {
        return ttl;
    }

    public void setTtl(Duration ttl) {
        this.ttl = ttl;
    }

    public Duration getIssueWindow() {
        return issueWindow;
    }

    public void setIssueWindow(Duration issueWindow) {
        this.issueWindow = issueWindow;
    }

    public Duration getLoginFailWindow() {
        return loginFailWindow;
    }

    public void setLoginFailWindow(Duration loginFailWindow) {
        this.loginFailWindow = loginFailWindow;
    }
}
