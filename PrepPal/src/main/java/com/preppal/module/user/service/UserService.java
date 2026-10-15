package com.preppal.module.user.service;

import com.preppal.module.user.dto.LoginResponse;

public interface UserService {

    /**
     * 微信登录。
     * 当前只实现 mock 通道（auth.mock-enabled=true 时 code=mock）；
     * 真实 jscode2session 流程见 UserServiceImpl 中的 TODO。
     */
    LoginResponse login(String code);
}
