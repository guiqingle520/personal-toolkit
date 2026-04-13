package com.personal.toolkit.auth.security;

import com.personal.toolkit.auth.config.JwtProperties;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证 JwtTokenService 在不同密钥配置下的启动行为，避免明文密钥被误判为非法 Base64 后直接导致服务启动失败。
 */
class JwtTokenServiceTest {

    /**
     * 非 Base64 但长度足够的明文密钥应回退为 UTF-8 字节构造签名 key，而不是在构造器阶段失败。
     */
    @Test
    void shouldAcceptPlainTextSecretWhenBase64DecodingFails() {
        JwtProperties properties = createProperties("change-me-change-me-change-me-change-me-change-me-1234567890");

        assertDoesNotThrow(() -> new JwtTokenService(properties));
    }

    /**
     * 过短的明文密钥应在启动阶段给出明确错误信息，避免生成弱密钥。
     */
    @Test
    void shouldRejectTooShortPlainTextSecret() {
        JwtProperties properties = createProperties("short-secret");

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new JwtTokenService(properties));

        assertEquals("JWT secret must be at least 32 bytes when provided as plain text or decoded from Base64", exception.getMessage());
    }

    /**
     * 空白密钥配置应回退到应用默认 Base64 密钥，避免本地环境把环境变量留空时导致服务无法启动。
     */
    @Test
    void shouldFallbackToDefaultSecretWhenConfiguredSecretIsBlank() {
        JwtProperties properties = createProperties("   ");

        assertDoesNotThrow(() -> new JwtTokenService(properties));
        assertEquals(JwtProperties.DEFAULT_SECRET, properties.getSecret());
    }

    private JwtProperties createProperties(String secret) {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(secret);
        properties.setIssuer("personal-toolkit-backend");
        properties.setExpiration(Duration.ofHours(12));
        return properties;
    }
}
