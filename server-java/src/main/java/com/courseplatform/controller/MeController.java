package com.courseplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.courseplatform.entity.Student;
import com.courseplatform.entity.StudentCourse;
import com.courseplatform.mapper.StudentMapper;
import com.courseplatform.mapper.StudentCourseMapper;
import com.courseplatform.security.AuthContext;
import com.courseplatform.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 当前账号（前端 GET /api/me） */
@RestController
public class MeController {

    private final StudentMapper studentMapper;
    private final StudentCourseMapper studentCourseMapper;

    public MeController(StudentMapper studentMapper, StudentCourseMapper studentCourseMapper) {
        this.studentMapper = studentMapper;
        this.studentCourseMapper = studentCourseMapper;
    }

    @GetMapping("/api/me")
    public Map<String, Object> me(HttpServletRequest req) {
        LoginUser u = AuthContext.current(req);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("role", u.getRole().toLowerCase());
        Map<String, Object> user = new LinkedHashMap<>();
        user.put("id", u.getUserId());
        user.put("name", u.getRealName());

        if ("STUDENT".equals(u.getRole())) {
            out.put("accountId", u.getStudentId());
            Student s = studentMapper.selectById(u.getStudentId());
            if (s != null) {
                user.put("studentNo", s.getStudentNo());
                user.put("grade", s.getGrade());
                user.put("major", s.getMajor());
                user.put("creditLimit", s.getCreditLimit());
                user.put("preferences", new LinkedHashMap<>());
            }
            long enrolled = studentCourseMapper.selectCount(new QueryWrapper<StudentCourse>()
                    .eq("student_id", u.getStudentId()).eq("status", "SELECTED"));
            List<Map<String, Object>> accounts = new ArrayList<>();
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("id", u.getStudentId());
            a.put("name", u.getRealName());
            a.put("studentNo", s == null ? u.getStudentNo() : s.getStudentNo());
            a.put("major", s == null ? u.getMajor() : s.getMajor());
            a.put("enrolledCount", enrolled);
            a.put("current", true);
            accounts.add(a);
            out.put("accounts", accounts);
        } else if ("TEACHER".equals(u.getRole())) {
            out.put("accountId", null);
            user.put("teacherNo", u.getTeacherNo());
            user.put("title", u.getTitle());
            user.put("dept", u.getDept());
            out.put("accounts", new ArrayList<>());
        } else {
            out.put("accountId", null);
            user.put("username", u.getUsername());
            out.put("accounts", new ArrayList<>());
        }
        out.put("user", user);

        // session / profile：与 Node 版 /api/me 响应结构对齐（前端 store.session / views.js 消费）
        Map<String, Object> session = new LinkedHashMap<>();
        session.put("userId", u.getUserId());
        session.put("username", u.getUsername());
        session.put("role", u.getRole());
        session.put("realName", u.getRealName());
        if ("STUDENT".equals(u.getRole())) {
            session.put("studentId", u.getStudentId());
            session.put("studentNo", u.getStudentNo());
            session.put("grade", u.getGrade());
            session.put("major", u.getMajor());
            session.put("creditLimit", u.getCreditLimit());
        } else if ("TEACHER".equals(u.getRole())) {
            session.put("teacherId", u.getTeacherId());
            session.put("teacherNo", u.getTeacherNo());
            session.put("title", u.getTitle());
            session.put("dept", u.getDept());
        }
        out.put("session", session);

        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("role", u.getRole().toLowerCase());
        profile.put("id", "STUDENT".equals(u.getRole()) ? u.getStudentId()
                : ("TEACHER".equals(u.getRole()) ? u.getTeacherId() : u.getUserId()));
        profile.put("name", u.getRealName());
        if ("STUDENT".equals(u.getRole())) profile.put("studentNo", u.getStudentNo());
        out.put("profile", profile);
        return out;
    }
}
