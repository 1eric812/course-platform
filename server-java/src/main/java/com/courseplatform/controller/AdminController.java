package com.courseplatform.controller;

import com.courseplatform.service.AdminService;
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

    public AdminController(AdminService adminService) {
        this.adminService = adminService;
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
}
