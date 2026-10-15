package com.preppal.common;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/** JWT 签发与解析（HS256） */
@Slf4j
@Component
public class JwtUtil {

    /** HS256 要求密钥至少 256 bit */
    private static final int MIN_SECRET_BYTES = 32;

    private final JwtProperties properties;

    public JwtUtil(JwtProperties properties) {
        this.properties = properties;
    }

    public String generate(Long userId) {
        SecretKey key = requireKey();
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuer(properties.getIssuer())
                .issuedAt(new Date(now))
                .expiration(new Date(now + properties.getExpiration() * 1000L))
                .signWith(key)
                .compact();
    }

    /** 解析失败或过期统一返回 null，由调用方转成 401 */
    public Long parseUserId(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(requireKey())
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
            return Long.valueOf(claims.getSubject());
        } catch (Exception e) {
            log.debug("JWT 解析失败: {}", e.getMessage());
            return null;
        }
    }

    private SecretKey requireKey() {
        String secret = properties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new BusinessException(ResultCode.SYSTEM_ERROR,
                    "未正确配置 auth.jwt.secret，请在 .env 中设置 JWT_SECRET（至少 32 个字符）");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }
}
