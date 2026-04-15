package com.personal.toolkit.auth.service;

import com.personal.toolkit.auth.dto.CaptchaResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 验证验证码服务的核心行为：可签发验证码、错误答案会失败。
 */
@ExtendWith(MockitoExtension.class)
class CaptchaServiceTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private CaptchaService captchaService;

    @BeforeEach
    void setUp() {
        captchaService = new CaptchaService(redisTemplate);
    }

    @Test
    void issueCaptchaShouldReturnSvgPayload() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        CaptchaResponse response = captchaService.issueCaptcha("127.0.0.1");

        assertNotNull(response.getCaptchaId());
        assertTrue(response.getCaptchaId().length() > 10);
        assertTrue(response.getImage().startsWith("data:image/svg+xml;base64,"));
        assertTrue(response.getExpiresInSeconds() > 0);

        verify(valueOperations).set(anyString(), any(Map.class), any());
    }

    @Test
    void clearLoginFailureShouldNotDeleteIpCounter() {
        captchaService.clearLoginFailure("127.0.0.1", "alice@example.com");

        verify(redisTemplate, never()).delete("auth:login:fail:ip:127.0.0.1");
    }

    @Test
    void validateCaptchaShouldFailWhenCaptchaMissing() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get(anyString())).thenReturn(null);

        assertThrows(ResponseStatusException.class,
                () -> captchaService.validateCaptcha("missing-id", "ABCDE"));
    }
}
