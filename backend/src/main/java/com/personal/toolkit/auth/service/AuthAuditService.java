package com.personal.toolkit.auth.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * 记录认证链路关键安全事件，便于后续联调排障与安全审计。
 */
@Service
public class AuthAuditService {

    private static final Logger log = LoggerFactory.getLogger(AuthAuditService.class);

    public void loginSucceeded(String clientIp, String loginIdentifier) {
        log.info("AUTH_AUDIT login_succeeded clientIp={} identifier={}", clientIp, safe(loginIdentifier));
    }

    public void loginFailed(String clientIp, String loginIdentifier, String reason) {
        log.warn("AUTH_AUDIT login_failed clientIp={} identifier={} reason={}", clientIp, safe(loginIdentifier), reason);
    }

    public void captchaIssued(String clientIp) {
        log.info("AUTH_AUDIT captcha_issued clientIp={}", clientIp);
    }

    public void captchaRejected(String clientIp, String reason) {
        log.warn("AUTH_AUDIT captcha_rejected clientIp={} reason={}", clientIp, reason);
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
