package com.courseplatform.config;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.courseplatform.entity.Student;
import com.courseplatform.entity.Teacher;
import com.courseplatform.exception.ApiException;
import com.courseplatform.mapper.StudentMapper;
import com.courseplatform.mapper.TeacherMapper;
import com.courseplatform.security.AuthContext;
import com.courseplatform.security.JwtUtil;
import com.courseplatform.security.LoginUser;
import com.courseplatform.security.TokenBlacklist;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** 登录鉴权 + 按角色接口隔离（对应需求 I-09 会话隔离） */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private static final Set<String> PUBLIC = new HashSet<>(Arrays.asList(
            "/api/auth/login", "/api/auth/logout", "/api/health", "/api/status"));

    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final MetricsRegistry metrics;

    public AuthInterceptor(JwtUtil jwtUtil, TokenBlacklist tokenBlacklist, StudentMapper studentMapper,
                           TeacherMapper teacherMapper, MetricsRegistry metrics) {
        this.jwtUtil = jwtUtil;
        this.tokenBlacklist = tokenBlacklist;
        this.studentMapper = studentMapper;
        this.teacherMapper = teacherMapper;
        this.metrics = metrics;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest req, @NonNull HttpServletResponse res, @NonNull Object handler) {
        String path = req.getRequestURI();
        // 运行监控口径：所有 /api/** 请求（含未登录被拦截的 401/403）都要计入
        metrics.recordRequest(path);
        if (PUBLIC.contains(path)) return true;

        String token = extractToken(req);
        if (token == null) throw new ApiException(401, "未登录或登录已过期，请重新登录", "AUTH_REQUIRED");
        if (tokenBlacklist.isRevoked(token)) throw new ApiException(401, "未登录或登录已过期，请重新登录", "AUTH_REQUIRED");

        Claims claims = jwtUtil.parse(token);
        LoginUser u = new LoginUser();
        u.setUserId(claims.get("userId", Long.class));
        u.setUsername(claims.getSubject());
        u.setRole(claims.get("role", String.class));
        u.setRealName(claims.get("realName", String.class));

        if ("STUDENT".equals(u.getRole())) {
            Student s = studentMapper.selectOne(new QueryWrapper<Student>().eq("user_id", u.getUserId()));
            if (s != null) {
                u.setStudentId(s.getId()); u.setStudentNo(s.getStudentNo());
                u.setGrade(s.getGrade()); u.setMajor(s.getMajor()); u.setCreditLimit(s.getCreditLimit());
            }
        } else if ("TEACHER".equals(u.getRole())) {
            Teacher t = teacherMapper.selectOne(new QueryWrapper<Teacher>().eq("user_id", u.getUserId()));
            if (t != null) {
                u.setTeacherId(t.getId()); u.setTeacherNo(t.getTeacherNo());
                u.setTitle(t.getTitle()); u.setDept(t.getDept());
            }
        }

        String need = requiredRole(path);
        if (need != null && !need.equals(u.getRole()))
            throw new ApiException(403, "无权限访问该端接口", "FORBIDDEN");

        req.setAttribute(AuthContext.ATTR, u);
        return true;
    }

    /**
     * Token 提取：请求头（Authorization / x-token）优先。
     *
     * <p>SSE（{@code EventSource}）与文件下载（课表 .ics、名单 .csv 由 {@code window.open} 打开）
     * 都是浏览器直接发起的请求，无法附加自定义请求头，故统一支持 {@code ?token=} 查询参数。</p>
     */
    private String extractToken(HttpServletRequest req) {
        String auth = req.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) return auth.substring(7).trim();
        String x = req.getHeader("x-token");
        if (x != null && !x.isBlank()) return x.trim();
        String q = req.getParameter("token");
        if (q != null && !q.isBlank()) return q.trim();
        return null;
    }

    /** 接口所需角色：null=任意已登录；STUDENT/TEACHER/ADMIN=仅该角色 */
    private String requiredRole(String path) {
        if (path.startsWith("/api/admin") || path.equals("/api/architecture")) return "ADMIN";
        if (path.startsWith("/api/teacher")) return "TEACHER";
        if (path.equals("/api/me") || path.startsWith("/api/preferences") || path.startsWith("/api/stream")) return null;
        return "STUDENT";
    }
}
