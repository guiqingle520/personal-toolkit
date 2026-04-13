package com.personal.toolkit.auth.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * 承载 JWT 签发所需的密钥、签发方和过期时间配置，供鉴权组件统一读取。
 */
@ConfigurationProperties(prefix = "app.auth.jwt")
public class JwtProperties {

    public static final String DEFAULT_SECRET = "uizFVR6Xqk4LzrbdSjOPDXz9r3mrI3FbTsT8z6jTexs=";

    private String secret = DEFAULT_SECRET;
    private String issuer;
    private Duration expiration = Duration.ofHours(12);

    /**
     * 返回 JWT 签名密钥原文配置。
     *
     * @return 对称签名密钥
     */
    public String getSecret() {
        return secret;
    }

    /**
     * 设置 JWT 签名密钥原文配置。
     *
     * @param secret 对称签名密钥
     */
    public void setSecret(String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            this.secret = DEFAULT_SECRET;
            return;
        }
        this.secret = secret;
    }

    /**
     * 返回 JWT issuer 配置值。
     *
     * @return 签发方标识
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * 设置 JWT issuer 配置值。
     *
     * @param issuer 签发方标识
     */
    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    /**
     * 返回 JWT 默认过期时长配置。
     *
     * @return 令牌过期时长
     */
    public Duration getExpiration() {
        return expiration;
    }

    /**
     * 设置 JWT 默认过期时长配置。
     *
     * @param expiration 令牌过期时长
     */
    public void setExpiration(Duration expiration) {
        this.expiration = expiration;
    }
}
