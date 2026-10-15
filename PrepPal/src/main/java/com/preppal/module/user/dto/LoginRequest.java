package com.preppal.module.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    /** wx.login 返回的 code；开发环境可用固定值 mock */
    @NotBlank(message = "缺少登录 code")
    private String code;
}
