package com.preppal.common;

import lombok.Getter;

/**
 * 统一响应码。
 * 约定：code = 0 表示成功；业务失败一律返回 HTTP 200 + 非 0 的 code，
 * 仅 UNAUTHORIZED 额外返回 HTTP 401，便于前端识别登录失效并跳转登录页。
 */
@Getter
public enum ResultCode {

    SUCCESS(0, "成功"),

    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "登录状态已失效，请重新登录"),
    FORBIDDEN(403, "无权访问该资源"),
    NOT_FOUND(404, "资源不存在"),
    FILE_TOO_LARGE(413, "单个文件不超过 5MB"),
    UNSUPPORTED_FILE(415, "仅支持 .txt、.md 文件"),

    BUSINESS_ERROR(500, "业务处理失败"),

    SYSTEM_ERROR(1000, "系统异常，请稍后重试"),
    AI_ERROR(1001, "AI 服务调用失败，请稍后重试"),
    AI_TIMEOUT(1002, "生成超时，可重试");

    private final int code;
    private final String message;

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
