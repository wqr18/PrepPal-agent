package com.preppal.common;

/**
 * 当前请求的登录用户上下文。
 * 由 AuthInterceptor 写入，afterCompletion 清理，禁止跨线程复用。
 */
public final class UserContext {

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    private UserContext() {
    }

    public static void set(Long userId) {
        HOLDER.set(userId);
    }

    public static Long get() {
        return HOLDER.get();
    }

    /** 取当前用户 ID，缺失则抛 401 */
    public static Long require() {
        Long userId = HOLDER.get();
        if (userId == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED);
        }
        return userId;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
