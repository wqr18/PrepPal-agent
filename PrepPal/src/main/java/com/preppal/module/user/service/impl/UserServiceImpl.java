package com.preppal.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.preppal.common.AuthProperties;
import com.preppal.common.BusinessException;
import com.preppal.common.JwtUtil;
import com.preppal.common.ResultCode;
import com.preppal.module.user.dto.LoginResponse;
import com.preppal.module.user.dto.UserVO;
import com.preppal.module.user.entity.User;
import com.preppal.module.user.mapper.UserMapper;
import com.preppal.module.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private static final String MOCK_CODE = "mock";
    private static final String MOCK_OPENID = "mock-openid";
    private static final String MOCK_NICKNAME = "王老师";

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final AuthProperties authProperties;
    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;

    @Override
    public LoginResponse login(String code) {
        String openid = resolveOpenid(code);
        User user = findByOpenid(openid);
        if (user == null) {
            user = createUser(openid);
        } else {
            user = backfillMockNickname(user);
        }
        return new LoginResponse(jwtUtil.generate(user.getId()), UserVO.from(user));
    }

    private User findByOpenid(String openid) {
        return userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getOpenid, openid));
    }

    private User createUser(String openid) {
        User user = new User();
        user.setOpenid(openid);
        if (MOCK_OPENID.equals(openid)) {
            user.setNickname(MOCK_NICKNAME);
        }
        userMapper.insert(user);
        // 依赖数据库默认值填充时间戳，重新读回保证返回值完整
        return userMapper.selectById(user.getId());
    }

    /** mock 账号可能是早期建号的（昵称为空），登录时补齐，避免前端显示为空 */
    private User backfillMockNickname(User user) {
        if (!MOCK_OPENID.equals(user.getOpenid()) || StringUtils.hasText(user.getNickname())) {
            return user;
        }
        user.setNickname(MOCK_NICKNAME);
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.updateById(user);
        return userMapper.selectById(user.getId());
    }

    private String resolveOpenid(String code) {
        if (MOCK_CODE.equals(code)) {
            if (!authProperties.isMockEnabled()) {
                throw new BusinessException(ResultCode.FORBIDDEN,
                        "Mock 登录未开启，请在 .env 中设置 MOCK_ENABLED=true 后重试");
            }
            return MOCK_OPENID;
        }
        return wxCodeToOpenid(code);
    }

    /**
     * 用 code 调用微信 jscode2session，换取 openid。
     * 响应：成功时 { openid, session_key, unionid? }；失败时 { errcode, errmsg }。
     * 常见 errcode：40029 code 无效/过期、45011 频率限制、40013 appid 非法、-1 系统繁忙。
     */
    private String wxCodeToOpenid(String code) {
        if (code == null || code.isBlank()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "登录 code 不能为空");
        }
        String appid = authProperties.getWx().getAppid();
        String secret = authProperties.getWx().getSecret();
        if (!StringUtils.hasText(appid) || !StringUtils.hasText(secret)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR,
                    "微信登录未配置，请在 .env 中设置 WX_APPID / WX_SECRET");
        }

        JsonNode node;
        try {
            // 微信返回的 Content-Type 是 text/plain（虽然内容为 JSON），
            // 不能直接 body(JsonNode.class)，须先读成 String 再手动解析。
            String bodyText = restClientBuilder
                    .build()
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .scheme("https")
                            .host("api.weixin.qq.com")
                            .path("/sns/jscode2session")
                            .queryParam("appid", appid)
                            .queryParam("secret", secret)
                            .queryParam("js_code", code)
                            .queryParam("grant_type", "authorization_code")
                            .build())
                    .retrieve()
                    .body(String.class);
            node = objectMapper.readTree(bodyText);
        } catch (Exception e) {
            log.error("jscode2session 调用失败", e);
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "微信登录服务调用失败，请稍后重试");
        }

        int errcode = node != null && node.has("errcode") ? node.get("errcode").asInt() : 0;
        if (node == null || errcode != 0) {
            String errmsg = node != null && node.has("errmsg") ? node.get("errmsg").asText() : "未知错误";
            throw new BusinessException(ResultCode.BUSINESS_ERROR, wxErrorMessage(errcode, errmsg));
        }

        String openid = node.get("openid").asText();
        if (!StringUtils.hasText(openid)) {
            throw new BusinessException(ResultCode.BUSINESS_ERROR, "微信登录未返回有效的 openid");
        }
        log.info("微信登录成功 openid={}", openid);
        return openid;
    }

    private String wxErrorMessage(int errcode, String errmsg) {
        String friendly;
        switch (errcode) {
            case 40029 -> friendly = "登录凭证已失效，请重新登录";
            case 45011 -> friendly = "操作过于频繁，请稍后再试";
            case 40013 -> friendly = "小程序 AppID 配置有误，请联系管理员";
            case -1 -> friendly = "微信服务繁忙，请稍后再试";
            default -> friendly = "微信登录失败";
        }
        return friendly + "（" + errcode + "）";
    }
}
