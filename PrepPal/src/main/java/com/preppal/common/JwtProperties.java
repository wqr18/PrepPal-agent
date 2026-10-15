package com.preppal.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** JWT 配置（auth.jwt.*） */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth.jwt")
public class JwtProperties {

    /** 签名密钥，由 .env 的 JWT_SECRET 注入，至少 32 个字符 */
    private String secret;

    /** 有效期（秒），默认 7 天 */
    private long expiration = 604800L;

    private String issuer = "preppal";
}
