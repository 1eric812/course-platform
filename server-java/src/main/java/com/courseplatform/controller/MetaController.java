package com.courseplatform.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.courseplatform.config.ServerClock;
import com.courseplatform.entity.SelectionPeriod;
import com.courseplatform.mapper.SelectionPeriodMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 服务状态 / 架构视图。
 *
 * <p>开放时刻（openAt）统一取自 {@link ServerClock}，与 SSE 推送使用同一时间基准，
 * 保证「抢课倒计时」在学生端各页面看到的是同一个值（需求 I-05 结果与名额的确定性反馈）。
 * 筛选项枚举 {@code GET /api/filters} 由 {@link CourseController} 提供（走 CourseService）。</p>
 */
@RestController
public class MetaController {

    private final ServerClock clock;
    private final SelectionPeriodMapper periodMapper;

    public MetaController(ServerClock clock, SelectionPeriodMapper periodMapper) {
        this.clock = clock;
        this.periodMapper = periodMapper;
    }

    @GetMapping("/api/status")
    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("serverTime", System.currentTimeMillis());
        m.put("openAt", clock.openAt());
        m.put("categories", List.of("必修", "选修", "通识", "体育"));
        m.put("phases", phases());
        return m;
    }

    /**
     * 选课阶段列表（需求 P-01-1：阶段名称 / 开放起止时间 / 当前所处阶段）。
     *
     * <p>表未初始化（本地库尚未导入 schema）或查询异常时返回空列表，首页自动隐藏节点卡片，
     * 不影响倒计时、待办等其它首页信息，保证接口可用性不依赖演示数据是否就绪。</p>
     */
    private List<Map<String, Object>> phases() {
        List<Map<String, Object>> out = new ArrayList<>();
        try {
            List<SelectionPeriod> rows = periodMapper.selectList(
                    new QueryWrapper<SelectionPeriod>().orderByAsc("start_time"));
            LocalDateTime now = LocalDateTime.now();
            for (SelectionPeriod p : rows) {
                boolean ongoing = p.getStartTime() != null && p.getEndTime() != null
                        && !now.isBefore(p.getStartTime()) && !now.isAfter(p.getEndTime());
                Map<String, Object> x = new LinkedHashMap<>();
                x.put("code", p.getPhaseCode());
                x.put("name", p.getPhaseName());
                x.put("start", p.getStartTime());
                x.put("end", p.getEndTime());
                x.put("status", ongoing ? "ONGOING" : p.getStatus());
                x.put("current", ongoing);
                x.put("remark", p.getRemark());
                out.add(x);
            }
        } catch (Exception e) {
            return List.of();
        }
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
                {"GET", "/api/status", "服务状态", "router"},
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
