package com.personal.toolkit.auth.service;

import com.personal.toolkit.auth.config.CaptchaProperties;
import com.personal.toolkit.auth.dto.CaptchaResponse;
import com.personal.toolkit.common.exception.ApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.springframework.util.StringUtils.hasText;

/**
 * 负责登录验证码的生成、存储、校验与登录失败频控。
 */
@Service
public class CaptchaService {

    private static final Logger log = LoggerFactory.getLogger(CaptchaService.class);

    private static final String CAPTCHA_KEY_PREFIX = "auth:captcha:";
    private static final String CAPTCHA_ISSUE_IP_PREFIX = "auth:captcha:issue:ip:";
    private static final String LOGIN_FAIL_IP_PREFIX = "auth:login:fail:ip:";
    private static final String LOGIN_FAIL_ID_PREFIX = "auth:login:fail:id:";

    private static final String CAPTCHA_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final RedisTemplate<String, Object> redisTemplate;
    private final CaptchaProperties captchaProperties;
    private final SecureRandom secureRandom = new SecureRandom();

    public CaptchaService(RedisTemplate<String, Object> redisTemplate,
                          CaptchaProperties captchaProperties) {
        this.redisTemplate = redisTemplate;
        this.captchaProperties = captchaProperties;
    }

    /**
     * 生成新的登录验证码并写入 Redis，返回可直接展示的 SVG Data URL。
     *
     * @return 验证码响应体
     */
    public CaptchaResponse issueCaptcha(String clientIp) {
        if (!captchaProperties.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Captcha is disabled");
        }

        enforceCaptchaIssueThrottle(clientIp);

        String captchaId = randomId(24);
        String answer = randomCaptchaText(captchaProperties.getLength());
        String answerHash = hashAnswer(answer);

        Map<String, Object> payload = Map.of(
                "answerHash", answerHash,
                "attemptsLeft", captchaProperties.getMaxAttempts()
        );

        redisTemplate.opsForValue().set(captchaRedisKey(captchaId), payload, captchaProperties.getTtl());
        log.info("Issued login captcha captchaId={} clientIp={} ttlSeconds={}", captchaId, clientIp, captchaProperties.getTtl().toSeconds());

        String svg = buildCaptchaSvg(answer);
        String image = "data:image/svg+xml;base64," + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
        return new CaptchaResponse(captchaId, image, (int) captchaProperties.getTtl().getSeconds());
    }

    public boolean isCaptchaEnabled() {
        return captchaProperties.isEnabled();
    }

    public boolean isAdaptiveCaptchaEnabled() {
        return captchaProperties.isEnabled() && captchaProperties.isAdaptive();
    }

    public int getAdaptiveTriggerThreshold() {
        return Math.max(captchaProperties.getAdaptiveTriggerThreshold(), 1);
    }

    /**
     * 校验验证码签发频率，避免匿名请求无限刷图导致资源滥用。
     *
     * @param clientIp 客户端 IP
     */
    public void enforceCaptchaIssueThrottle(String clientIp) {
        String key = captchaIssueIpKey(clientIp);
        long issueCount = readFailCount(key);
        if (issueCount >= captchaProperties.getIssueThreshold()) {
            log.warn("Blocked captcha issuance due to rate limit clientIp={} currentCount={} threshold={}", clientIp, issueCount, captchaProperties.getIssueThreshold());
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many captcha requests, please try again later");
        }

        Long current = redisTemplate.opsForValue().increment(key);
        if (current != null && current == 1L) {
            redisTemplate.expire(key, captchaProperties.getIssueWindow());
        }
    }

    /**
     * 校验登录前提交的验证码，失败会扣减尝试次数并在超限后删除验证码。
     *
     * @param captchaId 验证码标识
     * @param captchaCode 用户输入验证码
     */
    public void validateCaptcha(String captchaId, String captchaCode) {
        String redisKey = captchaRedisKey(captchaId);
        Object cached = redisTemplate.opsForValue().get(redisKey);
        if (!(cached instanceof Map<?, ?> map)) {
            log.warn("Rejected login captcha because it was missing or expired captchaId={}", captchaId);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Captcha expired or invalid");
        }

        String answerHash = asString(map.get("answerHash"));
        int attemptsLeft = asInteger(map.get("attemptsLeft"), captchaProperties.getMaxAttempts());
        String inputHash = hashAnswer(captchaCode);

        if (!inputHash.equals(answerHash)) {
            int nextAttemptsLeft = attemptsLeft - 1;
            if (nextAttemptsLeft <= 0) {
                redisTemplate.delete(redisKey);
                log.warn("Rejected login captcha after attempts exhausted captchaId={}", captchaId);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Captcha verification failed");
            }

            Map<String, Object> nextPayload = Map.of(
                    "answerHash", answerHash,
                    "attemptsLeft", nextAttemptsLeft
            );

            Long ttlSeconds = redisTemplate.getExpire(redisKey, TimeUnit.SECONDS);
            if (ttlSeconds == null || ttlSeconds <= 0) {
                redisTemplate.delete(redisKey);
                log.warn("Rejected login captcha because remaining TTL was invalid captchaId={}", captchaId);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Captcha expired or invalid");
            }

            redisTemplate.opsForValue().set(redisKey, nextPayload, Duration.ofSeconds(ttlSeconds));
            log.warn("Rejected login captcha due to answer mismatch captchaId={} attemptsLeft={}", captchaId, nextAttemptsLeft);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Captcha verification failed");
        }

        redisTemplate.delete(redisKey);
        log.info("Accepted login captcha captchaId={}", captchaId);
    }

    public boolean isCaptchaRequired(String clientIp, String loginIdentifier) {
        if (!captchaProperties.isEnabled()) {
            return false;
        }

        if (!captchaProperties.isAdaptive()) {
            return true;
        }

        long ipFails = readFailCount(loginFailIpKey(clientIp));
        long idFails = readFailCount(loginFailIdKey(normalizeIdentifier(loginIdentifier)));
        long threshold = getAdaptiveTriggerThreshold();
        return ipFails >= threshold || idFails >= threshold;
    }

    public void validateCaptchaIfNeeded(String clientIp,
                                        String loginIdentifier,
                                        String captchaId,
                                        String captchaCode) {
        boolean required = isCaptchaRequired(clientIp, loginIdentifier);
        if (!required) {
            if (hasText(captchaId) && hasText(captchaCode)) {
                validateCaptcha(captchaId, captchaCode);
            }
            return;
        }

        if (!hasText(captchaId) || !hasText(captchaCode)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "CAPTCHA_REQUIRED", "Captcha required before login");
        }

        validateCaptcha(captchaId, captchaCode);
    }

    /**
     * 在登录前检查是否触发频控阈值。
     *
     * @param clientIp 客户端 IP
     * @param loginIdentifier 登录标识（用户名或邮箱）
     */
    public void enforceLoginThrottle(String clientIp, String loginIdentifier) {
        long ipFails = readFailCount(loginFailIpKey(clientIp));
        long idFails = readFailCount(loginFailIdKey(normalizeIdentifier(loginIdentifier)));

        if (ipFails >= captchaProperties.getLoginFailThreshold() || idFails >= captchaProperties.getLoginFailThreshold()) {
            log.warn("Blocked login due to throttle clientIp={} identifier={} ipFails={} idFails={} threshold={}",
                    clientIp, normalizeIdentifier(loginIdentifier), ipFails, idFails, captchaProperties.getLoginFailThreshold());
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "Too many failed login attempts, please try again later");
        }
    }

    /**
     * 记录登录失败次数，用于后续频控。
     *
     * @param clientIp 客户端 IP
     * @param loginIdentifier 登录标识
     */
    public void recordLoginFailure(String clientIp, String loginIdentifier) {
        incrementWithTtl(loginFailIpKey(clientIp));
        incrementWithTtl(loginFailIdKey(normalizeIdentifier(loginIdentifier)));
        log.warn("Recorded login failure clientIp={} identifier={}", clientIp, normalizeIdentifier(loginIdentifier));
    }

    /**
     * 登录成功后重置频控计数。
     *
     * @param clientIp 客户端 IP
     * @param loginIdentifier 登录标识
     */
    public void clearLoginFailure(String clientIp, String loginIdentifier) {
        redisTemplate.delete(loginFailIdKey(normalizeIdentifier(loginIdentifier)));
        log.info("Cleared identifier login failure counter after successful login clientIp={} identifier={}", clientIp, normalizeIdentifier(loginIdentifier));
    }

    private void incrementWithTtl(String key) {
        Long current = redisTemplate.opsForValue().increment(key);
        if (current != null && current == 1L) {
            redisTemplate.expire(key, captchaProperties.getLoginFailWindow());
        }
    }

    private long readFailCount(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String str) {
            try {
                return Long.parseLong(str);
            } catch (NumberFormatException ignored) {
                return 0L;
            }
        }
        return 0L;
    }

    private String randomCaptchaText(int length) {
        StringBuilder builder = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            int index = secureRandom.nextInt(CAPTCHA_ALPHABET.length());
            builder.append(CAPTCHA_ALPHABET.charAt(index));
        }
        return builder.toString();
    }

    private String randomId(int bytesLength) {
        byte[] bytes = new byte[bytesLength];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hashAnswer(String answer) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(normalizeIdentifier(answer).getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 digest unavailable", ex);
        }
    }

    private String normalizeIdentifier(String input) {
        return input == null ? "" : input.trim().toUpperCase(Locale.ROOT);
    }

    private String captchaRedisKey(String captchaId) {
        return CAPTCHA_KEY_PREFIX + captchaId;
    }

    private String loginFailIpKey(String clientIp) {
        return LOGIN_FAIL_IP_PREFIX + (clientIp == null || clientIp.isBlank() ? "unknown" : clientIp);
    }

    private String loginFailIdKey(String loginIdentifier) {
        return LOGIN_FAIL_ID_PREFIX + loginIdentifier;
    }

    private String captchaIssueIpKey(String clientIp) {
        return CAPTCHA_ISSUE_IP_PREFIX + (clientIp == null || clientIp.isBlank() ? "unknown" : clientIp);
    }

    private String asString(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    private int asInteger(Object value, int fallback) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String str) {
            try {
                return Integer.parseInt(str);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private String buildCaptchaSvg(String answer) {
        int width = 140;
        int height = 48;

        StringBuilder svg = new StringBuilder();
        svg.append("<svg xmlns='http://www.w3.org/2000/svg' width='").append(width)
                .append("' height='").append(height).append("' viewBox='0 0 ").append(width).append(' ').append(height).append("'>");
        svg.append("<rect width='100%' height='100%' fill='#0f172a' rx='6' ry='6'/>");

        for (int i = 0; i < 4; i++) {
            int y1 = 8 + secureRandom.nextInt(height - 16);
            int y2 = 8 + secureRandom.nextInt(height - 16);
            svg.append("<line x1='0' y1='").append(y1)
                    .append("' x2='").append(width)
                    .append("' y2='").append(y2)
                    .append("' stroke='rgba(56,189,248,0.35)' stroke-width='1' />");
        }

        for (int i = 0; i < answer.length(); i++) {
            int x = 16 + i * 24;
            int y = 30 + secureRandom.nextInt(10) - 5;
            int rotate = secureRandom.nextInt(25) - 12;
            svg.append("<text x='").append(x)
                    .append("' y='").append(y)
                    .append("' fill='#e2e8f0' font-size='24' font-family='monospace' font-weight='700' transform='rotate(")
                    .append(rotate).append(' ').append(x).append(' ').append(y).append(")'>")
                    .append(answer.charAt(i)).append("</text>");
        }

        svg.append("</svg>");
        return svg.toString();
    }
}
