package com.courseplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.courseplatform.entity.SelectionPeriod;
import com.courseplatform.mapper.SelectionPeriodMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 选课阶段（selection_period）统一口径。
 *
 * <p>改造背景：原先「选课开放时刻」由 {@code ServerClock.openAt()} 给出，即
 * 「服务启动时间 + course-platform.demo.open-delay-ms」，是个纯粹为了演示倒计时而
 * 编造的假时刻——跟数据库里的 selection_period 表毫无关系。后果是教务端改了阶段
 * 时间前端毫无反应，首页「选课节点」卡片与倒计时也各说各话。</p>
 *
 * <p>现在把口径收敛到本服务：<b>阶段表是唯一真源</b>，
 * 倒计时锚点由 {@link #openAt()} 从阶段推导，阶段视图由 {@link #phases()} 输出。
 * {@code ServerClock} 保留，但只负责运行时长，不再承担「开放时刻」语义。</p>
 *
 * <p>容错：表未初始化或查询异常时 {@link #phases()} 返回空列表、{@link #openAt()}
 * 返回 {@code null}，前端据此隐藏倒计时，而不是显示一个凭空的数字。</p>
 */
@Service
public class PeriodService {

    private final SelectionPeriodMapper periodMapper;

    public PeriodService(SelectionPeriodMapper periodMapper) {
        this.periodMapper = periodMapper;
    }

    /** 按开始时间升序取全部阶段；查询失败返回空列表（不抛异常，保证接口可用性） */
    public List<SelectionPeriod> list() {
        try {
            return periodMapper.selectList(
                    new QueryWrapper<SelectionPeriod>().orderByAsc("start_time"));
        } catch (Exception e) {
            return List.of();
        }
    }

    /** 当前时刻所在阶段（无则 null） */
    public SelectionPeriod current() {
        LocalDateTime now = LocalDateTime.now();
        for (SelectionPeriod p : list()) {
            if (p.getStartTime() != null && p.getEndTime() != null
                    && !now.isBefore(p.getStartTime()) && !now.isAfter(p.getEndTime())) {
                return p;
            }
        }
        return null;
    }

    /** 下一个尚未开始的阶段（按开始时间最近，无则 null） */
    public SelectionPeriod next() {
        LocalDateTime now = LocalDateTime.now();
        for (SelectionPeriod p : list()) {
            if (p.getStartTime() != null && p.getStartTime().isAfter(now)) {
                return p;
            }
        }
        return null;
    }

    /**
     * 选课开放时刻（epoch 毫秒），倒计时的唯一锚点。
     *
     * <p>有进行中的阶段 → 取它的开始时间（已开放，前端倒计时归零）；
     * 否则取下一个未开始阶段的开始时间；阶段表为空 → 返回 null。
     * 返回 null 时前端不展示倒计时，避免出现「假的 90 秒」。</p>
     */
    public Long openAt() {
        SelectionPeriod cur = current();
        if (cur != null && cur.getStartTime() != null) {
            return toMillis(cur.getStartTime());
        }
        SelectionPeriod nxt = next();
        if (nxt != null && nxt.getStartTime() != null) {
            return toMillis(nxt.getStartTime());
        }
        return null;
    }

    /**
     * 阶段视图，字段与静态版（docs/js/backend/data.js#phaseViews）保持一致，
     * 使前端两端可以共用同一套消费逻辑。
     */
    public List<Map<String, Object>> phases() {
        List<Map<String, Object>> out = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (SelectionPeriod p : list()) {
            boolean ongoing = p.getStartTime() != null && p.getEndTime() != null
                    && !now.isBefore(p.getStartTime()) && !now.isAfter(p.getEndTime());
            boolean finished = p.getEndTime() != null && now.isAfter(p.getEndTime());
            Map<String, Object> x = new LinkedHashMap<>();
            x.put("code", p.getPhaseCode());
            x.put("name", p.getPhaseName());
            x.put("start", p.getStartTime());
            x.put("end", p.getEndTime());
            x.put("status", ongoing ? "ONGOING" : finished ? "FINISHED" : "NOT_STARTED");
            x.put("current", ongoing);
            x.put("remark", p.getRemark());
            out.add(x);
        }
        return out;
    }

    /**
     * 调整某阶段起止时间（教务端）。
     *
     * <p>改完后 {@link #openAt()} 立刻反映新值，学生端下次拉 /api/status 就能看到
     * 新倒计时，无需重启服务。</p>
     */
    public Map<String, Object> updatePeriod(String code, Map<String, Object> patch) {
        SelectionPeriod p = periodMapper.selectOne(
                new QueryWrapper<SelectionPeriod>().eq("phase_code", String.valueOf(code).toUpperCase()));
        if (p == null) {
            throw new com.courseplatform.exception.ApiException(400, "阶段不存在：" + code, null);
        }
        Object st = patch.get("startTime");
        Object et = patch.get("endTime");
        if (st instanceof String && !((String) st).isBlank()) {
            p.setStartTime(LocalDateTime.parse(((String) st).trim().replace(" ", "T")));
        }
        if (et instanceof String && !((String) et).isBlank()) {
            p.setEndTime(LocalDateTime.parse(((String) et).trim().replace(" ", "T")));
        }
        if (patch.get("remark") != null) {
            p.setRemark(String.valueOf(patch.get("remark")));
        }
        if (p.getEndTime() != null && p.getStartTime() != null
                && !p.getEndTime().isAfter(p.getStartTime())) {
            p.setEndTime(p.getStartTime().plusHours(1));
        }
        periodMapper.updateById(p);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("items", phases());
        return out;
    }

    private static long toMillis(LocalDateTime t) {
        return t.atZone(java.time.ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}
