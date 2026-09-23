package com.courseplatform.controller;

import com.courseplatform.service.TeacherService;
import com.courseplatform.security.AuthContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 教师端 */
@RestController
@RequestMapping("/api/teacher")
public class TeacherController {

    private final TeacherService teacherService;

    public TeacherController(TeacherService teacherService) {
        this.teacherService = teacherService;
    }

    @GetMapping("/me")
    public Map<String, Object> me(HttpServletRequest req) {
        return teacherService.profile(AuthContext.current(req));
    }

    @GetMapping("/courses")
    public Map<String, Object> courses(HttpServletRequest req) {
        return teacherService.courses(AuthContext.current(req));
    }

    @GetMapping("/courses/{id}/roster")
    public Map<String, Object> roster(@PathVariable Long id, HttpServletRequest req) {
        return teacherService.roster(id, AuthContext.current(req));
    }

    @GetMapping("/courses/{id}/roster.csv")
    public void rosterCsv(@PathVariable Long id, HttpServletRequest req, HttpServletResponse res) throws IOException {
        Map<String, Object> r = teacherService.roster(id, AuthContext.current(req));
        @SuppressWarnings("unchecked")
        var students = (java.util.List<Map<String, Object>>) r.get("students");
        var course = (Map<String, Object>) r.get("course");
        String lines = "学号,姓名,年级,专业,状态\n"
                + students.stream()
                    .map(s -> String.join(",", String.valueOf(s.get("studentNo")), String.valueOf(s.get("name")),
                            String.valueOf(s.get("grade")), String.valueOf(s.get("major")), String.valueOf(s.get("status"))))
                    .collect(Collectors.joining("\n"));
        res.setContentType("text/csv; charset=utf-8");
        res.setHeader("Content-Disposition", "attachment; filename=\"roster_" + course.get("code") + ".csv\"");
        res.getOutputStream().write("\uFEFF".getBytes(StandardCharsets.UTF_8));
        res.getOutputStream().write(lines.getBytes(StandardCharsets.UTF_8));
    }

    @PutMapping("/courses/{id}")
    public Map<String, Object> update(@PathVariable Long id, @RequestBody Map<String, Object> body, HttpServletRequest req) {
        return teacherService.updateCourse(id, body, AuthContext.current(req));
    }

    /* ================= 学业闭环 ================= */

    @GetMapping("/overview")
    public Map<String, Object> overview(HttpServletRequest req) {
        return teacherService.overview(AuthContext.current(req));
    }

    @GetMapping("/courses/{id}/scores")
    public Map<String, Object> courseScores(@PathVariable Long id, HttpServletRequest req) {
        return teacherService.courseScores(AuthContext.current(req), id);
    }

    @PutMapping("/courses/{id}/scores")
    public Map<String, Object> saveScores(@PathVariable Long id, @RequestBody Map<String, Object> body,
                                          HttpServletRequest req) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
        return teacherService.saveScores(AuthContext.current(req), id, items);
    }

    @GetMapping("/courses/{id}/stats")
    public Map<String, Object> courseStats(@PathVariable Long id) {
        return teacherService.courseStats(id);
    }

    @GetMapping("/courses/{id}/failures")
    public Map<String, Object> failList(@PathVariable Long id) {
        return teacherService.failList(id);
    }

    @GetMapping("/timetable")
    public Map<String, Object> teacherTimetable(HttpServletRequest req) {
        return teacherService.teacherTimetable(AuthContext.current(req));
    }
}
