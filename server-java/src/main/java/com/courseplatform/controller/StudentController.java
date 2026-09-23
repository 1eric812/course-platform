package com.courseplatform.controller;

import com.courseplatform.security.AuthContext;
import com.courseplatform.security.LoginUser;
import com.courseplatform.service.StudentService;
import com.courseplatform.service.TimetableService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** 学生端：心愿单 / 选课 / 课表 / 消息 / 偏好 / 放号订阅 */
@RestController
@RequestMapping("/api")
public class StudentController {

    private final StudentService studentService;
    private final TimetableService timetableService;

    public StudentController(StudentService studentService, TimetableService timetableService) {
        this.studentService = studentService;
        this.timetableService = timetableService;
    }

    @GetMapping("/wishlist")
    public Map<String, Object> wishlist(HttpServletRequest req) {
        return studentService.wishlist(AuthContext.current(req));
    }

    @PostMapping("/wishlist")
    public Map<String, Object> wishlistAdd(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        Long courseId = Long.valueOf(String.valueOf(body.get("courseId")));
        return studentService.wishlistAdd(courseId, AuthContext.current(req));
    }

    @DeleteMapping("/wishlist/{id}")
    public Map<String, Object> wishlistRemove(@PathVariable Long id, HttpServletRequest req) {
        return studentService.wishlistRemove(id, AuthContext.current(req));
    }

    @PutMapping("/wishlist/reorder")
    public Map<String, Object> reorder(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        List<Long> ids = ((List<?>) body.get("orderedIds")).stream()
                .map(x -> Long.valueOf(String.valueOf(x))).collect(java.util.stream.Collectors.toList());
        return studentService.wishlistReorder(ids, AuthContext.current(req));
    }

    @PostMapping("/selection/submit")
    public Map<String, Object> submit(HttpServletRequest req) {
        return studentService.submitSelection(AuthContext.current(req));
    }

    @GetMapping("/selection/status/{id}")
    public Map<String, Object> ticket(@PathVariable Long id, HttpServletRequest req) {
        return studentService.ticketStatus(id, AuthContext.current(req));
    }

    @GetMapping("/timetable")
    public Map<String, Object> timetable(HttpServletRequest req) {
        return studentService.timetable(AuthContext.current(req));
    }

    /** 课表导出 .ics 日历文件（前端 window.open 直接下载） */
    @GetMapping(value = "/timetable.ics", produces = "text/calendar;charset=UTF-8")
    public ResponseEntity<byte[]> timetableIcs(HttpServletRequest req) {
        LoginUser u = AuthContext.current(req);
        String ics = timetableService.exportIcs(u.getStudentId() == null ? 0L : u.getStudentId());
        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"timetable.ics\"")
                .contentType(MediaType.parseMediaType("text/calendar;charset=UTF-8"))
                .body(ics.getBytes(StandardCharsets.UTF_8));
    }

    @GetMapping("/messages")
    public Map<String, Object> messages(HttpServletRequest req) {
        return studentService.messages(AuthContext.current(req));
    }

    @PostMapping("/messages/read")
    public Map<String, Object> readMessages(HttpServletRequest req) {
        return studentService.markRead(AuthContext.current(req));
    }

    @GetMapping("/preferences")
    public Map<String, Object> preferences(HttpServletRequest req) {
        return studentService.preferences(AuthContext.current(req));
    }

    @PutMapping("/preferences")
    public Map<String, Object> setPreferences(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        return studentService.setPreferences(body, AuthContext.current(req));
    }

    @GetMapping("/subscriptions")
    public Map<String, Object> subscriptions(HttpServletRequest req) {
        return studentService.subscriptions(AuthContext.current(req));
    }

    @PostMapping("/subscribe")
    public Map<String, Object> subscribe(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        return studentService.subscribe(Long.valueOf(String.valueOf(body.get("courseId"))), AuthContext.current(req));
    }

    @PostMapping("/unsubscribe")
    public Map<String, Object> unsubscribe(@RequestBody Map<String, Object> body, HttpServletRequest req) {
        return studentService.unsubscribe(Long.valueOf(String.valueOf(body.get("courseId"))), AuthContext.current(req));
    }

    /* ================= 学业闭环 ================= */

    @GetMapping("/student/scores")
    public Map<String, Object> studentScores(@RequestParam(required = false) Long semesterId, HttpServletRequest req) {
        return studentService.studentScores(AuthContext.current(req), semesterId);
    }

    @GetMapping("/student/score-history")
    public Map<String, Object> scoreHistory(HttpServletRequest req) {
        return studentService.studentScoreHistory(AuthContext.current(req));
    }

    @GetMapping("/student/credits")
    public Map<String, Object> credits(HttpServletRequest req) {
        return studentService.studentCredits(AuthContext.current(req));
    }

    @GetMapping("/student/records")
    public Map<String, Object> records(@RequestParam(required = false) Long semesterId, HttpServletRequest req) {
        return studentService.records(AuthContext.current(req).getStudentId(), semesterId);
    }

    @GetMapping("/student/announcements")
    public Map<String, Object> announcements(HttpServletRequest req) {
        return studentService.announcements();
    }

    @GetMapping("/student/timetables")
    public Map<String, Object> timetables(@RequestParam(required = false) Long semesterId, HttpServletRequest req) {
        return studentService.timetables(AuthContext.current(req), semesterId);
    }
}
