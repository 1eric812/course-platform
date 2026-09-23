package com.courseplatform.support;

/**
 * 业务异常：统一承载 HTTP 状态码、错误文案与可选错误码。
 * 401 未登录场景会携带 code=AUTH_REQUIRED，前端据此跳回登录页。
 */
public class ApiException extends RuntimeException {

    private final int status;
    private final String code;

    public ApiException(int status, String message) {
        this(status, message, null);
    }

    public ApiException(int status, String message, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }

    public static ApiException badRequest(String message) {
        return new ApiException(400, message);
    }

    public static ApiException unauthorized(String message) {
        return new ApiException(401, message);
    }

    public static ApiException authRequired() {
        return new ApiException(401, "登录状态已失效，请重新登录", "AUTH_REQUIRED");
    }

    public static ApiException forbidden(String message) {
        return new ApiException(403, message);
    }

    public static ApiException notFound(String message) {
        return new ApiException(404, message);
    }
}
