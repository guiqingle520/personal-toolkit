package com.personal.toolkit.auth.security;

import com.personal.toolkit.auth.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.io.DecodingException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

/**
 * 负责签发与解析 JWT 访问令牌，承载当前版本的无状态鉴权能力。
 */
@Service
public class JwtTokenService {

    private final JwtProperties jwtProperties;
    private final SecretKey secretKey;

    public JwtTokenService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = resolveSigningKey(jwtProperties.getSecret());
    }

    /**
     * 为当前登录用户签发 JWT，令牌中携带用户主键与用户名用于后续请求鉴权。
     *
     * @param principal 当前登录用户主体
     * @return 新签发的 JWT 令牌
     */
    public String generateToken(AppUserPrincipal principal) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(jwtProperties.getExpiration());
        return Jwts.builder()
                .subject(principal.getUsername())
                .issuer(jwtProperties.getIssuer())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("userId", principal.getId())
                .signWith(secretKey)
                .compact();
    }

    /**
     * 解析并校验 JWT，成功时返回声明集合供过滤器继续加载用户上下文。
     *
     * @param token 待解析 JWT 令牌
     * @return 已校验的声明集合
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(jwtProperties.getIssuer())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 判断 JWT 是否可被成功解析且满足签名与过期校验。
     *
     * @param token 待校验 JWT 令牌
     * @return 是否为有效令牌
     */
    public boolean isValid(String token) {
        try {
            parseToken(token);
            return true;
        } catch (JwtException | IllegalArgumentException ex) {
            return false;
        }
    }

    /**
     * 从配置密钥中构造签名密钥，优先尝试 Base64 解码，失败时退回原始字符串字节。
     *
     * @param secret 原始密钥配置
     * @return HMAC 签名密钥
     */
    private SecretKey resolveSigningKey(String secret) {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret must not be blank");
        }

        try {
            return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
        } catch (IllegalArgumentException | DecodingException ex) {
            return buildUtf8SigningKey(secret);
        }
    }

    /**
     * 当密钥不是合法 Base64 文本时，回退为按原始 UTF-8 文本构造 HMAC 密钥，
     * 同时在长度不足时给出更可读的启动失败原因。
     *
     * @param secret 原始密钥配置
     * @return HMAC 签名密钥
     */
    private SecretKey buildUtf8SigningKey(String secret) {
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes when provided as plain text or decoded from Base64");
        }
        return Keys.hmacShaKeyFor(secretBytes);
    }
}
