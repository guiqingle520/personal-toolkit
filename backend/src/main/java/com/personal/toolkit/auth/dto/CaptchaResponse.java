package com.personal.toolkit.auth.dto;

/**
 * 描述登录验证码接口返回的前端可渲染数据。
 */
public class CaptchaResponse {

    private String captchaId;
    private String image;
    private int expiresInSeconds;

    public CaptchaResponse() {
    }

    public CaptchaResponse(String captchaId, String image, int expiresInSeconds) {
        this.captchaId = captchaId;
        this.image = image;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getCaptchaId() {
        return captchaId;
    }

    public void setCaptchaId(String captchaId) {
        this.captchaId = captchaId;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public int getExpiresInSeconds() {
        return expiresInSeconds;
    }

    public void setExpiresInSeconds(int expiresInSeconds) {
        this.expiresInSeconds = expiresInSeconds;
    }
}
