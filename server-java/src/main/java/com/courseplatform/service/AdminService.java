package com.courseplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.courseplatform.config.MetricsRegistry;
import com.courseplatform.config.ServerClock;
import com.courseplatform.entity.AnomalyTicket;
import com.courseplatform.entity.Course;
import com.courseplatform.entity.SelectionRule;
import com.courseplatform.entity.SelectionTicket;
import com.courseplatform.entity.StudentCourse;
import com.courseplatform.exception.ApiException;
import com.courseplatform.mapper.AnomalyTicketMapper;
import com.courseplatform.mapper.CourseMapper;
import com.courseplatform.mapper.SelectionRuleMapper;
import com.courseplatform.mapper.SelectionTicketMapper;
import com.courseplatform.mapper.StudentCourseMapper;
import com.courseplatform.mapper.WishlistMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptException;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 教务管理端 */
@Service
public class AdminService {

    private final CourseMapper courseMapper;
    private final SelectionRuleMapper ruleMapper;
    private final AnomalyTicketMapper anomalyMapper;
    private final SelectionTicketMapper ticketMapper;
    private final StudentCourseMapper studentCourseMapper;
    private final WishlistMapper wishlistMapper;
    private final ServerClock clock;
    private final MetricsRegistry metrics;
    private final JdbcTemplate jdbcTemplate;

    public AdminService(CourseMapper courseMapper, SelectionRuleMapper ruleMapper, AnomalyTicketMapper anomalyMapper,
                        SelectionTicketMapper ticketMapper, StudentCourseMapper studentCourseMapper,
                        WishlistMapper wishlistMapper, ServerClock clock, MetricsRegistry metrics,
                        JdbcTemplate jdbcTemplate) {
        this.courseMapper = courseMapper;
        this.ruleMapper = ruleMapper;
        this.anomalyMapper = anomalyMapper;
        this.ticketMapper = ticketMapper;
        this.studentCourseMapper = studentCourseMapper;
        this.wishlistMapper = wishlistMapper;
        this.clock = clock;
        this.metrics = metrics;
        this.jdbcTemplate = jdbcTemplate;
    }

    private Map<String, Object> ruleMap(SelectionRule r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("selectionOpen", r.getSelectionOpen() != null && r.getSelectionOpen() == 1);
        m.put("creditLimit", r.getCreditLimit());
        m.put("maxWishlist", r.getMaxWishlist());
        m.put("allowCrossCampus", r.getAllowCrossCampus() != null && r.getAllowCrossCampus() == 1);
        m.put("blockOnConflict", r.getBlockOnConflict() != null && r.getBlockOnConflict() == 1);
        return m;
    }

    public Map<String, Object> overview() {
        List<Course> cs = courseMapper.selectList(null);
        int cap = cs.stream().mapToInt(c -> c.getCapacity() == null ? 0 : c.getCapacity()).sum();
        int enr = cs.stream().mapToInt(c -> c.getEnrolled() == null ? 0 : c.getEnrolled()).sum();
        long pending = anomalyMapper.selectCount(new QueryWrapper<AnomalyTicket>().eq("status", "PENDING"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("courseCount", cs.size());
        out.put("totalCapacity", cap);
        out.put("totalEnrolled", enr);
        out.put("seatsRemaining", cap - enr);
        out.put("fillRate", cap == 0 ? 0 : Math.round(enr * 100.0f / cap));
        out.put("pendingAnomalies", pending);
        out.put("totalRequests", metrics.getTotalRequests());
        out.put("uptime", clock.uptimeSeconds());
        out.put("rules", ruleMap(ruleMapper.selectById(1L)));
        return out;
    }

    public Map<String, Object> rules() {
        return ruleMap(ruleMapper.selectById(1L));
    }

    @Transactional
    public Map<String, Object> updateRules(Map<String, Object> patch) {
        SelectionRule r = ruleMapper.selectById(1L);
        if (patch.get("selectionOpen") != null) r.setSelectionOpen(Boolean.TRUE.equals(patch.get("selectionOpen")) ? 1 : 0);
        if (patch.get("allowCrossCampus") != null) r.setAllowCrossCampus(Boolean.TRUE.equals(patch.get("allowCrossCampus")) ? 1 : 0);
        if (patch.get("blockOnConflict") != null) r.setBlockOnConflict(Boolean.TRUE.equals(patch.get("blockOnConflict")) ? 1 : 0);
        if (patch.get("creditLimit") != null) r.setCreditLimit(new BigDecimal(String.valueOf(patch.get("creditLimit"))));
        if (patch.get("maxWishlist") != null) r.setMaxWishlist(Integer.valueOf(String.valueOf(patch.get("maxWishlist"))));
        r.setUpdatedAt(LocalDateTime.now());
        ruleMapper.updateById(r);
        return ruleMap(r);
    }

    /** 运行监控：服务运行态 + 接口调用指标 + 业务计数（教务端「运行监控」页） */
    public Map<String, Object> monitor() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("startedAt", clock.startedAt());
        out.put("uptime", clock.uptimeSeconds());
        out.put("node", "Java " + ManagementFactory.getRuntimeMXBean().getVmVersion() + " · Spring Boot");
        long mb = (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1048576;
        out.put("memoryMB", mb);
        out.put("totalRequests", metrics.getTotalRequests());
        out.put("errors", metrics.getErrors());
        out.put("sseClients", metrics.getSseClients());
        out.put("tickets", ticketMapper.selectCount(null));
        out.put("wishlistSize", wishlistMapper.selectCount(null));
        out.put("enrolledCount", studentCourseMapper.selectCount(new QueryWrapper<StudentCourse>().eq("status", "SELECTED")));
        out.put("byPath", metrics.ranking(10));
        out.put("recentTickets", recentTickets());
        return out;
    }

    /** 最近 5 张受理票据（运行监控页表格，状态与 /api/selection/status 的中文口径保持一致） */
    private List<Map<String, Object>> recentTickets() {
        List<SelectionTicket> list = ticketMapper.selectList(new QueryWrapper<SelectionTicket>()
                .orderByDesc("id").last("LIMIT 5"));
        List<Map<String, Object>> out = new ArrayList<>();
        for (SelectionTicket t : list) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getId());
            m.put("status", zhStatus(t.getStatus()));
            m.put("accepted", t.getAcceptedCount() == null ? 0 : t.getAcceptedCount());
            m.put("rejected", t.getRejectedCount() == null ? 0 : t.getRejectedCount());
            m.put("total", t.getTotalCount() == null ? 0 : t.getTotalCount());
            out.add(m);
        }
        return out;
    }

    private String zhStatus(String s) {
        if (s == null) return "未知";
        return switch (s) {
            case "ACCEPTED" -> "已受理";
            case "PROCESSING" -> "处理中";
            case "SUCCESS", "PARTIAL" -> "成功";
            case "FAILED" -> "失败";
            default -> s;
        };
    }

    public Map<String, Object> anomalies(String status) {
        QueryWrapper<AnomalyTicket> qw = new QueryWrapper<>();
        if (status != null && (status.equals("pending") || status.equals("resolved")))
            qw.eq("status", status.equals("pending") ? "PENDING" : "RESOLVED");
        qw.orderByDesc("created_at");
        List<Map<String, Object>> items = anomalyMapper.selectList(qw).stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId());
            m.put("type", a.getType());
            m.put("student", (a.getStudentNo() == null ? "" : a.getStudentNo()) + " " + (a.getStudentName() == null ? "" : a.getStudentName()));
            m.put("courseId", a.getCourseId());
            m.put("courseName", a.getCourseName());
            m.put("reason", a.getReason());
            m.put("time", a.getCreatedAt() == null ? "" : a.getCreatedAt().toString().replace("T", " ").substring(0, 16));
            m.put("status", a.getStatus() == null || a.getStatus().equals("PENDING") ? "pending" : "resolved");
            m.put("resolution", a.getResolution());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        return out;
    }

    @Transactional
    public Map<String, Object> resolveAnomaly(Long id, String action) {
        AnomalyTicket a = anomalyMapper.selectById(id);
        if (a == null) throw new ApiException(404, "工单不存在", null);
        if ("RESOLVED".equals(a.getStatus())) throw new ApiException(400, "该工单已处理", null);
        if ("dismiss".equals(action)) {
            a.setStatus("RESOLVED");
            a.setResolution("已标记为处理完成");
            a.setHandledAt(LocalDateTime.now());
            anomalyMapper.updateById(a);
        } else if ("force".equals(action)) {
            if (a.getCourseId() != null) {
                Course c = courseMapper.selectById(a.getCourseId());
                if (c == null) throw new ApiException(400, "课程不存在", null);
                c.setEnrolled(c.getEnrolled() + 1); // 教务强制补选，允许超出容量
                courseMapper.updateById(c);
            }
            a.setStatus("RESOLVED");
            a.setResolution("已强制处理");
            a.setHandledAt(LocalDateTime.now());
            anomalyMapper.updateById(a);
        } else {
            throw new ApiException(400, "未知操作", null);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getId());
        m.put("status", "resolved");
        out.put("anomaly", m);
        return out;
    }

    @Transactional
    public Map<String, Object> adjustSeats(Long courseId, Integer delta) {
        Course c = courseMapper.selectById(courseId);
        if (c == null) throw new ApiException(404, "课程不存在", null);
        if (delta == null || delta == 0) throw new ApiException(400, "调整量不能为 0", null);
        int newCap = c.getCapacity() + delta;
        if (newCap < c.getEnrolled()) throw new ApiException(400, "容量不能低于已选人数（" + c.getEnrolled() + "）", null);
        c.setCapacity(newCap);
        courseMapper.updateById(c);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("capacity", c.getCapacity());
        m.put("enrolled", c.getEnrolled());
        m.put("remaining", c.getRemaining());
        out.put("course", m);
        return out;
    }

    /**
     * 重置演示数据：把「活动数据」恢复为 data.sql 的初始状态，并清零运行监控指标。
     *
     * <p>活动表 = 课程与排课、选课记录、心愿单、放号订阅、消息、异常工单、受理票据；
     * 账号（sys_user / student / teacher）、选课规则、选课时段、偏好设置等配置类数据保持不变。
     * 脚本 {@code db/reset-demo-data.sql} 在事务内执行，失败则整体回滚。</p>
     */
    @Transactional
    public Map<String, Object> reset() {
        DataSource ds = jdbcTemplate.getDataSource();
        if (ds == null) throw new ApiException(500, "数据源不可用，无法重置演示数据", null);
        EncodedResource script = new EncodedResource(new ClassPathResource("db/reset-demo-data.sql"),
                StandardCharsets.UTF_8);
        try (Connection cn = ds.getConnection()) {
            ScriptUtils.executeSqlScript(cn, script);
        } catch (SQLException | ScriptException e) {
            throw new ApiException(500, "重置演示数据失败：" + e.getMessage(), null);
        }
        metrics.reset();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("message", "演示数据已重置为初始状态");
        out.put("courseCount", courseMapper.selectCount(null));
        out.put("enrolledCount", studentCourseMapper.selectCount(new QueryWrapper<StudentCourse>().eq("status", "SELECTED")));
        out.put("wishlistSize", wishlistMapper.selectCount(null));
        out.put("tickets", ticketMapper.selectCount(null));
        out.put("pendingAnomalies", anomalyMapper.selectCount(new QueryWrapper<AnomalyTicket>().eq("status", "PENDING")));
        return out;
    }
}
