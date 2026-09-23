package com.courseplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.courseplatform.entity.TrainingPlan;
import com.courseplatform.exception.ApiException;
import com.courseplatform.mapper.TrainingPlanMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 培养方案：毕业学分标准的 CRUD 与按专业年级查询 */
@Service
public class TrainingPlanService {

    private final TrainingPlanMapper planMapper;

    public TrainingPlanService(TrainingPlanMapper planMapper) {
        this.planMapper = planMapper;
    }

    private Map<String, Object> toMap(TrainingPlan p) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getId());
        m.put("major", p.getMajor());
        m.put("grade", p.getGrade());
        m.put("semesterId", p.getSemesterId());
        m.put("requiredCourses", p.getRequiredCourses());
        m.put("electiveCourses", p.getElectiveCourses());
        m.put("minCredits", p.getMinCredits());
        m.put("maxCredits", p.getMaxCredits());
        m.put("gradTotalCredits", p.getGradTotalCredits());
        m.put("gradRequiredCredits", p.getGradRequiredCredits());
        m.put("gradElectiveCredits", p.getGradElectiveCredits());
        m.put("genEduCredits", p.getGenEduCredits());
        m.put("remark", p.getRemark());
        return m;
    }

    public Map<String, Object> listAll() {
        List<Map<String, Object>> items = new ArrayList<>();
        for (TrainingPlan p : planMapper.selectList(new QueryWrapper<TrainingPlan>().orderByAsc("major", "grade"))) {
            items.add(toMap(p));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("total", items.size());
        return out;
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        String major = str(body.get("major"));
        String grade = str(body.get("grade"));
        if (major.isEmpty()) throw new ApiException(400, "专业（major）不能为空", null);
        if (grade.isEmpty()) throw new ApiException(400, "年级（grade）不能为空", null);
        TrainingPlan p = new TrainingPlan();
        p.setMajor(major);
        p.setGrade(grade);
        p.setSemesterId(body.get("semesterId") == null ? null : Long.valueOf(String.valueOf(body.get("semesterId"))));
        p.setRequiredCourses(body.get("requiredCourses") == null ? null : String.valueOf(body.get("requiredCourses")));
        p.setElectiveCourses(body.get("electiveCourses") == null ? null : String.valueOf(body.get("electiveCourses")));
        p.setMinCredits(decimal(body.get("minCredits")));
        p.setMaxCredits(decimal(body.get("maxCredits")));
        p.setGradTotalCredits(decimal(body.get("gradTotalCredits")));
        p.setGradRequiredCredits(decimal(body.get("gradRequiredCredits")));
        p.setGradElectiveCredits(decimal(body.get("gradElectiveCredits")));
        p.setGenEduCredits(decimal(body.get("genEduCredits")));
        p.setRemark(str(body.get("remark")).isEmpty() ? null : str(body.get("remark")));
        p.setCreatedAt(LocalDateTime.now());
        p.setUpdatedAt(LocalDateTime.now());
        planMapper.insert(p);
        return toMap(p);
    }

    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        TrainingPlan p = planMapper.selectById(id);
        if (p == null) throw new ApiException(404, "培养方案不存在", null);
        if (body.get("major") != null) p.setMajor(str(body.get("major")));
        if (body.get("grade") != null) p.setGrade(str(body.get("grade")));
        if (body.get("semesterId") != null) p.setSemesterId(Long.valueOf(String.valueOf(body.get("semesterId"))));
        if (body.get("requiredCourses") != null) p.setRequiredCourses(String.valueOf(body.get("requiredCourses")));
        if (body.get("electiveCourses") != null) p.setElectiveCourses(String.valueOf(body.get("electiveCourses")));
        if (body.get("minCredits") != null) p.setMinCredits(decimal(body.get("minCredits")));
        if (body.get("maxCredits") != null) p.setMaxCredits(decimal(body.get("maxCredits")));
        if (body.get("gradTotalCredits") != null) p.setGradTotalCredits(decimal(body.get("gradTotalCredits")));
        if (body.get("gradRequiredCredits") != null) p.setGradRequiredCredits(decimal(body.get("gradRequiredCredits")));
        if (body.get("gradElectiveCredits") != null) p.setGradElectiveCredits(decimal(body.get("gradElectiveCredits")));
        if (body.get("genEduCredits") != null) p.setGenEduCredits(decimal(body.get("genEduCredits")));
        if (body.get("remark") != null) p.setRemark(str(body.get("remark")).isEmpty() ? null : str(body.get("remark")));
        p.setUpdatedAt(LocalDateTime.now());
        planMapper.updateById(p);
        return toMap(p);
    }

    /** 按专业 + 年级查询培养方案（无则返回 null） */
    public TrainingPlan getByMajorGrade(String major, String grade) {
        return planMapper.selectOne(new QueryWrapper<TrainingPlan>()
                .eq("major", major).eq("grade", grade).last("LIMIT 1"));
    }

    private String str(Object v) { return v == null ? "" : String.valueOf(v).trim(); }

    private BigDecimal decimal(Object v) {
        if (v == null || String.valueOf(v).isBlank()) return null;
        return new BigDecimal(String.valueOf(v));
    }
}
