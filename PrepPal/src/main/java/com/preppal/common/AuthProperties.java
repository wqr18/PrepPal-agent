package com.preppal.common;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 鉴权相关配置（auth.*）。
 * 集中声明后，IDE 能通过 spring-boot-configuration-processor 生成元数据，
 * 不再对 auth.wx.* / auth.mock-enabled 报 unknown property。
 */
@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "auth")
public class AuthProperties {

    /** 是否允许 mock 登录（仅 dev），由 .env 的 MOCK_ENABLED 注入 */
    private boolean mockEnabled = false;

    private Wx wx = new Wx();

    @Getter
    @Setter
    public static class Wx {

        /** 小程序 appid，接入真实 wx.login 时配置 */
        private String appid;

        /** 小程序 secret，接入真实 wx.login 时配置 */
        private String secret;
    }
}
