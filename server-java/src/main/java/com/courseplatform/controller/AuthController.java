package com.courseplatform.controller;

import com.courseplatform.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 多端登录 / 登出。
 *
 * <p>「当前账号」统一由 {@link MeController} 的 {@code GET /api/me} 提供
 * （返回 session + profile + accounts 三份结构，前端 store 与视图均消费该结构）。
 * 本控制器不再重复暴露 {@code /api/auth/me}：一方面避免同一职责存在两套响应结构，
 * 另一方面原 {@code /api/auth/me} 因不在拦截器白名单内、默认按学生端断言，教师端访问会被 403。</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        String username = body.get("username") == null ? null : String.valueOf(body.get("username"));
        String password = body.get("password") == null ? null : String.valueOf(body.get("password"));
        String role = body.get("role") == null ? null : String.valueOf(body.get("role"));
        return authService.login(username, password, role);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout() {
        return authService.logout();
    }
}
