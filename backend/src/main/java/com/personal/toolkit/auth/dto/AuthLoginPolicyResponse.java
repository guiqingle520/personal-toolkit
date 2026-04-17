package com.personal.toolkit.auth.dto;

/**
 * 描述前端登录页初始化所需的验证码策略。
 */
public class AuthLoginPolicyResponse {

    private final boolean captchaEnabled;
    private final boolean adaptiveCaptcha;
    private final int adaptiveTriggerThreshold;

    public AuthLoginPolicyResponse(boolean captchaEnabled, boolean adaptiveCaptcha, int adaptiveTriggerThreshold) {
        this.captchaEnabled = captchaEnabled;
        this.adaptiveCaptcha = adaptiveCaptcha;
        this.adaptiveTriggerThreshold = adaptiveTriggerThreshold;
    }

    public boolean isCaptchaEnabled() {
        return captchaEnabled;
    }

    public boolean isAdaptiveCaptcha() {
        return adaptiveCaptcha;
    }

    public int getAdaptiveTriggerThreshold() {
        return adaptiveTriggerThreshold;
    }
}
