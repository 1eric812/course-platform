package com.courseplatform.controller;

import com.courseplatform.entity.SelectionRule;
import com.courseplatform.mapper.SelectionRuleMapper;
import com.courseplatform.service.PeriodService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务状态 / 架构视图。
 *
 * <p>开放时刻（openAt）与选课阶段（phases）统一由 {@link PeriodService} 从
 * selection_period 表推导，三端看到的是同一个值（需求 I-05 结果与名额的确定性反馈，
 * P-01-1 选课节点卡片）。原先这里用的是 {@code ServerClock.openAt()}，即
 * 「启动时间 + 演示延迟」，与数据库无关，已废弃该口径。</p>
 *
 * <p>筛选项枚举 {@code GET /api/filters} 由 {@link CourseController} 提供（走 CourseService）。</p>
 */
@RestController
public class MetaController {

    private final PeriodService periodService;
    private final SelectionRuleMapper ruleMapper;

    public MetaController(PeriodService periodService, SelectionRuleMapper ruleMapper) {
        this.periodService = periodService;
        this.ruleMapper = ruleMapper;
    }

    @GetMapping("/api/status")
    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("serverTime", System.currentTimeMillis());
        /* openAt 为 null 表示阶段表未配置 → 前端不显示倒计时（而不是显示假数字） */
        m.put("openAt", periodService.openAt());
        m.put("stages", periodService.phases());
        m.put("categories", List.of("必修", "选修", "通识", "体育"));
        m.put("phases", periodService.phases());
        m.put("selectionOpen", selectionOpen());
        return m;
    }

    /**
     * 选课通道开关（selection_rule.selection_open）。
     *
     * <p>与「是否在阶段窗口内」是两个独立条件：教务可以手动关闸，
     * 学生端据此把提交按钮置灰并给出说明。查询失败时按「开放」处理，
     * 避免因配置表缺失而误锁整个选课流程。</p>
     */
    private boolean selectionOpen() {
        try {
            SelectionRule r = ruleMapper.selectById(1L);
            return r == null || r.getSelectionOpen() == null || r.getSelectionOpen() == 1;
        } catch (Exception e) {
            return true;
        }
    }

    /** 选课阶段列表（P-01-1：阶段名称 / 开放起止时间 / 当前所处阶段） */
    @GetMapping("/api/periods")
    public Map<String, Object> periods() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", periodService.phases());
        return out;
    }

    /** 接口清单（教务端「架构视图」页；鉴权由拦截器按 /api/admin、/api/architecture 限定 ADMIN）。 */
    @GetMapping("/api/architecture")
    public Map<String, Object> architecture() {
        List<Map<String, Object>> eps = new ArrayList<>();
        String[][] rows = {
                {"POST", "/api/auth/login", "多端密码登录", "auth"},
                {"POST", "/api/auth/logout", "登出", "auth"},
                {"GET", "/api/health", "健康检查", "router"},
                {"GET", "/api/status", "服务状态（含选课阶段与开放时刻）", "router"},
                {"GET", "/api/periods", "选课阶段时间表", "service"},
                {"GET", "/api/filters", "筛选项枚举", "service"},
                {"GET", "/api/courses", "课程列表", "service"},
                {"GET", "/api/courses/:id", "课程详情", "service"},
                {"GET", "/api/wishlist", "心愿单", "service"},
                {"POST", "/api/wishlist", "加入心愿单", "service"},
                {"DELETE", "/api/wishlist/:id", "移出心愿单", "service"},
                {"PUT", "/api/wishlist/reorder", "志愿排序", "service"},
                {"POST", "/api/selection/submit", "提交选课", "service"},
                {"GET", "/api/selection/status/:ticket", "受理结果", "service"},
                {"GET", "/api/timetable", "个人课表", "service"},
                {"GET", "/api/timetable.ics", "课表 ICS 导出", "router"},
                {"GET", "/api/messages", "消息中心", "service"},
                {"POST", "/api/messages/read", "全部已读", "service"},
                {"GET", "/api/preferences", "读取设置", "service"},
                {"PUT", "/api/preferences", "写入设置", "service"},
                {"GET", "/api/subscriptions", "放号订阅", "service"},
                {"POST", "/api/subscribe", "订阅放号", "service"},
                {"POST", "/api/unsubscribe", "取消订阅", "service"},
                {"GET", "/api/me", "当前账号", "service"},
                {"GET", "/api/teacher/me", "教师档案", "service"},
                {"GET", "/api/teacher/courses", "授课课程", "service"},
                {"GET", "/api/teacher/courses/:id/roster", "选课名单", "service"},
                {"GET", "/api/teacher/courses/:id/roster.csv", "名单 CSV 导出", "router"},
                {"PUT", "/api/teacher/courses/:id", "维护课程", "service"},
                {"GET", "/api/admin/overview", "教务总览", "service"},
                {"GET", "/api/admin/rules", "读取规则", "service"},
                {"PUT", "/api/admin/rules", "更新规则", "service"},
                {"GET", "/api/admin/monitor", "运行监控", "service"},
                {"GET", "/api/admin/anomalies", "异常工单", "service"},
                {"POST", "/api/admin/anomalies/:id/resolve", "处理工单", "service"},
                {"POST", "/api/admin/courses/:id/seats", "调整容量", "service"},
                {"POST", "/api/admin/reset", "重置演示数据", "service"},
                {"GET", "/api/stream/seats", "SSE 名额推送", "router"},
        };
        for (String[] r : rows) {
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("method", r[0]);
            x.put("path", r[1]);
            x.put("desc", r[2]);
            x.put("module", r[3]);
            eps.add(x);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("endpoints", eps);
        return out;
    }
}
