package com.courseplatform.exception;

import com.courseplatform.config.MetricsRegistry;
import io.jsonwebtoken.JwtException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/** 全局异常处理：把异常统一转为 { error, code?, detail? } JSON，并计入运行监控的异常计数 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final MetricsRegistry metrics;

    public GlobalExceptionHandler(MetricsRegistry metrics) {
        this.metrics = metrics;
    }

    private Map<String, Object> body(int status, String error, String code, String detail) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("error", error);
        if (code != null) m.put("code", code);
        if (detail != null) m.put("detail", detail);
        return m;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<Map<String, Object>> api(ApiException e) {
        metrics.recordError();
        return ResponseEntity.status(e.getStatus())
                .body(body(e.getStatus(), e.getMessage(), e.getCode(), null));
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<Map<String, Object>> jwt(JwtException e) {
        metrics.recordError();
        return ResponseEntity.status(401)
                .body(body(401, "登录已失效或 Token 无效，请重新登录", "AUTH_REQUIRED", e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> generic(Exception e) {
        metrics.recordError();
        log.warn("接口异常", e);
        return ResponseEntity.status(500)
                .body(body(500, "服务器内部错误", null, e.getMessage() == null ? null : e.getMessage()));
    }
}
