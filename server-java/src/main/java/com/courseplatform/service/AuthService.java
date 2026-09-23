package com.courseplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.courseplatform.entity.Student;
import com.courseplatform.entity.SysUser;
import com.courseplatform.entity.Teacher;
import com.courseplatform.exception.ApiException;
import com.courseplatform.mapper.StudentMapper;
import com.courseplatform.mapper.SysUserMapper;
import com.courseplatform.mapper.TeacherMapper;
import com.courseplatform.security.JwtUtil;
import com.courseplatform.security.TokenBlacklist;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/** 多端密码登录服务 */
@Service
public class AuthService {

    private final SysUserMapper sysUserMapper;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final JwtUtil jwtUtil;
    private final TokenBlacklist tokenBlacklist;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(SysUserMapper sysUserMapper, StudentMapper studentMapper,
                       TeacherMapper teacherMapper, JwtUtil jwtUtil, TokenBlacklist tokenBlacklist) {
        this.sysUserMapper = sysUserMapper;
        this.studentMapper = studentMapper;
        this.teacherMapper = teacherMapper;
        this.jwtUtil = jwtUtil;
        this.tokenBlacklist = tokenBlacklist;
    }

    @Transactional
    public Map<String, Object> login(String username, String password, String role) {
        if (username == null || username.isBlank() || password == null || password.isBlank()) {
            throw new ApiException(401, "请输入账号和密码", "BAD_REQUEST");
        }
        String r = (role == null ? "" : role.toUpperCase());
        if (!r.matches("STUDENT|TEACHER|ADMIN")) throw new ApiException(401, "请选择正确的登录端", "BAD_REQUEST");

        SysUser u = sysUserMapper.selectOne(new QueryWrapper<SysUser>()
                .eq("username", username.trim()).eq("role", r).eq("deleted", 0));
        if (u == null) throw new ApiException(401, "账号不存在，或所选端与账号不匹配", "LOGIN_FAILED");
        if (u.getStatus() == null || u.getStatus() != 1) throw new ApiException(401, "该账号已停用，请联系管理员", "LOGIN_FAILED");
        if (!encoder.matches(password, u.getPassword())) throw new ApiException(401, "密码错误", "LOGIN_FAILED");

        String token = jwtUtil.generate(u.getId(), u.getUsername(), u.getRole(), u.getRealName());
        u.setLastLoginAt(LocalDateTime.now());
        sysUserMapper.updateById(u);

        Map<String, Object> session = new LinkedHashMap<>();
        session.put("userId", u.getId());
        session.put("username", u.getUsername());
        session.put("role", u.getRole());
        session.put("realName", u.getRealName());

        if ("STUDENT".equals(u.getRole())) {
            Student s = studentMapper.selectOne(new QueryWrapper<Student>().eq("user_id", u.getId()));
            if (s != null) {
                session.put("studentId", s.getId());
                session.put("studentNo", s.getStudentNo());
                session.put("grade", s.getGrade());
                session.put("major", s.getMajor());
                session.put("creditLimit", s.getCreditLimit());
            }
        } else if ("TEACHER".equals(u.getRole())) {
            Teacher t = teacherMapper.selectOne(new QueryWrapper<Teacher>().eq("user_id", u.getId()));
            if (t != null) {
                session.put("teacherId", t.getId());
                session.put("teacherNo", t.getTeacherNo());
                session.put("title", t.getTitle());
                session.put("dept", t.getDept());
            }
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("token", token);
        out.put("session", session);
        return out;
    }

    /** 退出登录：把当前请求携带的 token 加入黑名单（logout 在公开白名单内，token 可能缺失或已过期，均容错） */
    public Map<String, Object> logout() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs != null) {
            HttpServletRequest req = attrs.getRequest();
            String auth = req.getHeader("Authorization");
            String token = (auth != null && auth.startsWith("Bearer ")) ? auth.substring(7).trim() : req.getHeader("x-token");
            if (token == null || token.isBlank()) token = req.getParameter("token");
            if (token != null && !token.isBlank()) {
                try {
                    Claims claims = jwtUtil.parse(token);
                    tokenBlacklist.revoke(token, claims.getExpiration().getTime());
                } catch (Exception ignored) {
                    // token 非法或已过期：无需加入黑名单，直接视为登出成功
                }
            }
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        return out;
    }
}
