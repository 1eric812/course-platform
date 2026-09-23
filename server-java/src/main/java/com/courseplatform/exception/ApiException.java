package com.courseplatform.exception;

/** 业务/鉴权异常：携带 HTTP 状态码与错误码，由 GlobalExceptionHandler 统一返回 */
public class ApiException extends RuntimeException {
    private final int status;
    private final String code;

    public ApiException(int status, String message, String code) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public int getStatus() { return status; }
    public String getCode() { return code; }
}
