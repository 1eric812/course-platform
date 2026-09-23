package com.courseplatform.security;

import com.courseplatform.exception.ApiException;
import jakarta.servlet.http.HttpServletRequest;

/** 从请求上下文中读取当前登录用户 */
public final class AuthContext {
    public static final String ATTR = "loginUser";

    private AuthContext() {}

    public static LoginUser current(HttpServletRequest req) {
        Object v = req.getAttribute(ATTR);
        if (!(v instanceof LoginUser)) {
            throw new ApiException(401, "未登录或登录已过期，请重新登录", "AUTH_REQUIRED");
        }
        return (LoginUser) v;
    }
}
