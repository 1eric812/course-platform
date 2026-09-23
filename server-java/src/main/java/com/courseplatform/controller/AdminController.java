package com.courseplatform.controller;

import com.courseplatform.service.AdminService;
import com.courseplatform.service.PeriodService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 教务管理端 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminService adminService;
    private final PeriodService periodService;

    public AdminController(AdminService adminService, PeriodService periodService) {
        this.adminService = adminService;
        this.periodService = periodService;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        return adminService.overview();
    }

    @GetMapping("/rules")
    public Map<String, Object> rules() {
        return adminService.rules();
    }

    @PutMapping("/rules")
    public Map<String, Object> updateRules(@RequestBody Map<String, Object> body) {
        return adminService.updateRules(body);
    }

    /**
     * 调整选课阶段起止时间（P-01-1）。
     *
     * <p>改完后 /api/status 的 openAt 与 phases 立刻反映新值，
     * 学生端倒计时无需重启服务即可跟随。</p>
     */
    @PutMapping("/periods/{code}")
    public Map<String, Object> updatePeriod(@PathVariable String code, @RequestBody Map<String, Object> body) {
        return periodService.updatePeriod(code, body);
    }

    @GetMapping("/monitor")
    public Map<String, Object> monitor() {
        return adminService.monitor();
    }

    @GetMapping("/anomalies")
    public Map<String, Object> anomalies(@RequestParam(required = false) String status) {
        return adminService.anomalies(status);
    }

    @PostMapping("/anomalies/{id}/resolve")
    public Map<String, Object> resolve(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String action = body.get("action") == null ? null : String.valueOf(body.get("action"));
        return adminService.resolveAnomaly(id, action);
    }

    @PostMapping("/courses/{id}/seats")
    public Map<String, Object> seats(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Integer delta = body.get("delta") == null ? 0 : Integer.valueOf(String.valueOf(body.get("delta")));
        return adminService.adjustSeats(id, delta);
    }

    @PostMapping("/reset")
    public Map<String, Object> reset() {
        return adminService.reset();
    }

    /* ================= 学业闭环 ================= */

    @GetMapping("/semesters")
    public Map<String, Object> semesters() {
        return adminService.semesters();
    }

    @PostMapping("/semesters")
    public Map<String, Object> createSemester(@RequestBody Map<String, Object> body) {
        return adminService.createSemester(body);
    }

    @PutMapping("/semesters/{id}")
    public Map<String, Object> updateSemester(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return adminService.updateSemester(id, body);
    }

    @PostMapping("/semesters/{id}/archive")
    public Map<String, Object> archiveSemester(@PathVariable Long id) {
        return adminService.archiveSemester(id);
    }

    @GetMapping("/training-plans")
    public Map<String, Object> trainingPlans() {
        return adminService.trainingPlans();
    }

    @PostMapping("/training-plans")
    public Map<String, Object> createTrainingPlan(@RequestBody Map<String, Object> body) {
        return adminService.createTrainingPlan(body);
    }

    @PutMapping("/training-plans/{id}")
    public Map<String, Object> updateTrainingPlan(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return adminService.updateTrainingPlan(id, body);
    }

    @GetMapping("/courses")
    public Map<String, Object> allCourses(@RequestParam(required = false) Long semesterId,
                                          @RequestParam(required = false) String category) {
        return adminService.allCourses(semesterId, category);
    }

    @PostMapping("/courses")
    public Map<String, Object> createCourse(@RequestBody Map<String, Object> body) {
        return adminService.createCourse(body);
    }

    @PutMapping("/courses/{id}")
    public Map<String, Object> updateCourse(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return adminService.updateCourse(id, body);
    }

    @GetMapping("/scores")
    public Map<String, Object> scores(@RequestParam(required = false) Long semesterId,
                                      @RequestParam(required = false) String status) {
        return adminService.scores(semesterId, status);
    }

    @PutMapping("/scores/{id}/audit")
    public Map<String, Object> auditScore(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        String action = body.get("action") == null ? null : String.valueOf(body.get("action"));
        return adminService.auditScore(id, action);
    }

    @PostMapping("/credits/settle")
    public Map<String, Object> creditsSettle(@RequestBody(required = false) Map<String, Object> body) {
        Long semesterId = body == null || body.get("semesterId") == null ? null
                : Long.valueOf(String.valueOf(body.get("semesterId")));
        return adminService.creditsSettle(semesterId);
    }

    @GetMapping("/graduation")
    public Map<String, Object> graduation() {
        return adminService.graduation();
    }

    @GetMapping("/announcements")
    public Map<String, Object> announcements() {
        return adminService.announcements();
    }

    @PostMapping("/announcements")
    public Map<String, Object> createAnnouncement(@RequestBody Map<String, Object> body) {
        return adminService.createAnnouncement(body);
    }

    @PutMapping("/announcements/{id}")
    public Map<String, Object> updateAnnouncement(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        return adminService.updateAnnouncement(id, body);
    }

    @GetMapping("/logs")
    public Map<String, Object> logs(@RequestParam(required = false) Integer page,
                                    @RequestParam(required = false) Integer size) {
        return adminService.logs(page, size);
    }
}
