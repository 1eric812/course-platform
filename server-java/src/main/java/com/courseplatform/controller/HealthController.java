package com.courseplatform.controller;

import com.courseplatform.config.ServerClock;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 健康检查接口：与前端 {@code client/js/api.js} 的 {@code GET /api/health} 契约保持一致，
 * 用于验证 8080 端口与数据源是否正常（不依赖具体业务表，未导入数据时也能返回）。
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    private final ServerClock clock;
    private final DataSource dataSource;

    public HealthController(ServerClock clock, DataSource dataSource) {
        this.clock = clock;
        this.dataSource = dataSource;
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ok", true);
        body.put("service", "course-platform");
        body.put("db", dbProduct());
        body.put("serverTime", System.currentTimeMillis());
        body.put("uptime", clock.uptimeSeconds());
        body.put("time", LocalDateTime.now().toString());
        return body;
    }

    /** 数据源产品名，仅用于健康检查展示；连不上时返回 unavailable，不阻断 200 响应。 */
    private String dbProduct() {
        try (Connection cn = dataSource.getConnection()) {
            return cn.getMetaData().getDatabaseProductName().toLowerCase().contains("mysql") ? "mysql" : "other";
        } catch (Exception e) {
            return "unavailable";
        }
    }
}
