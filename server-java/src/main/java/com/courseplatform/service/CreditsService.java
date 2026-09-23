package com.courseplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.courseplatform.entity.Course;
import com.courseplatform.entity.Score;
import com.courseplatform.entity.Student;
import com.courseplatform.entity.TrainingPlan;
import com.courseplatform.exception.ApiException;
import com.courseplatform.mapper.CourseMapper;
import com.courseplatform.mapper.ScoreMapper;
import com.courseplatform.mapper.StudentMapper;
import com.courseplatform.mapper.TrainingPlanMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 学分结算体系：学期结算、学生学分总览、毕业进度、毕业统计 */
@Service
public class CreditsService {

    private final ScoreMapper scoreMapper;
    private final CourseMapper courseMapper;
    private final StudentMapper studentMapper;
    private final TrainingPlanMapper planMapper;
    private final SemesterService semesterService;
    private final OperationLogService operationLogService;

    public CreditsService(ScoreMapper scoreMapper, CourseMapper courseMapper, StudentMapper studentMapper,
                          TrainingPlanMapper planMapper, SemesterService semesterService,
                          OperationLogService operationLogService) {
        this.scoreMapper = scoreMapper;
        this.courseMapper = courseMapper;
        this.studentMapper = studentMapper;
        this.planMapper = planMapper;
        this.semesterService = semesterService;
        this.operationLogService = operationLogService;
    }

    private boolean isRequired(String category) {
        if (category == null) return false;
        return category.contains("必修") || category.contains("实践") || category.contains("体育");
    }

    private boolean isGenEdu(String category) {
        if (category == null) return false;
        return category.contains("通识");
    }

    /**
     * 学期学分结算：遍历该学期「已审核/已归档」成绩中获学分课程，
     * 按课程类别累加到学生 required / elective / genEdu，重算总学分、不及格学分、加权绩点，
     * 并清零本学期已选学分。
     */
    @Transactional
    public Map<String, Object> settleSemester(Long semesterId) {
        if (semesterId == null) {
            var s = semesterService.getCurrent();
            if (s == null) throw new ApiException(400, "不存在任何学期，无法结算", null);
            semesterId = s.getId();
        }
        List<Score> scores = scoreMapper.selectList(new QueryWrapper<Score>()
                .eq("semester_id", semesterId)
                .in("status", "已审核", "已归档"));
        Map<Long, List<Score>> byStudent = new LinkedHashMap<>();
        for (Score sc : scores) {
            byStudent.computeIfAbsent(sc.getStudentId(), k -> new ArrayList<>()).add(sc);
        }
        int settled = 0;
        for (Map.Entry<Long, List<Score>> e : byStudent.entrySet()) {
            Student st = studentMapper.selectById(e.getKey());
            if (st == null) continue;
            BigDecimal required = BigDecimal.ZERO, elective = BigDecimal.ZERO, genEdu = BigDecimal.ZERO;
            BigDecimal failed = BigDecimal.ZERO;
            BigDecimal gpaSum = BigDecimal.ZERO, creditSum = BigDecimal.ZERO;
            for (Score sc : e.getValue()) {
                Course c = courseMapper.selectById(sc.getCourseId());
                if (c == null || c.getCredits() == null) continue;
                BigDecimal credits = c.getCredits();
                boolean obtained = sc.getCreditObtained() != null && sc.getCreditObtained() == 1;
                if (obtained) {
                    if (isGenEdu(c.getCategory())) genEdu = genEdu.add(credits);
                    else if (isRequired(c.getCategory())) required = required.add(credits);
                    else elective = elective.add(credits);
                } else {
                    failed = failed.add(credits);
                }
                BigDecimal gpa = sc.getCourseGpa() == null ? BigDecimal.ZERO : sc.getCourseGpa();
                gpaSum = gpaSum.add(credits.multiply(gpa));
                creditSum = creditSum.add(credits);
            }
            BigDecimal avgGpa = creditSum.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : gpaSum.divide(creditSum, 2, RoundingMode.HALF_UP);
            st.setRequiredEarnedCredits(required);
            st.setElectiveEarnedCredits(elective);
            st.setGenEduEarnedCredits(genEdu);
            st.setTotalEarnedCredits(required.add(elective).add(genEdu));
            st.setFailedCredits(failed);
            st.setTotalGpa(avgGpa);
            st.setAvgGpa(avgGpa);
            st.setCurrentSelectedCredits(BigDecimal.ZERO);
            st.setCurrentEarnedCredits(BigDecimal.ZERO);
            studentMapper.updateById(st);
            settled++;
        }
        operationLogService.write(42L, "admin", "学分结算", "学期结算", "结算学期(semester_id=" + semesterId + ") 学生 " + settled + " 人");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("semesterId", semesterId);
        out.put("settledStudents", settled);
        out.put("totalRecords", scores.size());
        return out;
    }

    /** 学生学分总览：本学期已选/已修、累计、分类进度、绩点、毕业进度 */
    public Map<String, Object> studentCredits(Long studentId) {
        Student st = studentMapper.selectById(studentId);
        if (st == null) throw new ApiException(404, "学生不存在", null);
        BigDecimal currentSelected = nz(st.getCurrentSelectedCredits());
        BigDecimal currentEarned = nz(st.getCurrentEarnedCredits());
        BigDecimal totalEarned = nz(st.getTotalEarnedCredits());
        BigDecimal required = nz(st.getRequiredEarnedCredits());
        BigDecimal elective = nz(st.getElectiveEarnedCredits());
        BigDecimal genEdu = nz(st.getGenEduEarnedCredits());
        BigDecimal failed = nz(st.getFailedCredits());
        BigDecimal totalGpa = nz(st.getTotalGpa());
        BigDecimal avgGpa = nz(st.getAvgGpa());
        TrainingPlan plan = planMapper.selectOne(new QueryWrapper<TrainingPlan>()
                .eq("major", st.getMajor() == null ? "" : st.getMajor())
                .eq("grade", st.getGrade() == null ? "" : st.getGrade()).last("LIMIT 1"));
        BigDecimal gradTotal = plan == null || plan.getGradTotalCredits() == null
                ? BigDecimal.ZERO : plan.getGradTotalCredits();
        BigDecimal gradRequired = plan == null || plan.getGradRequiredCredits() == null
                ? BigDecimal.ZERO : plan.getGradRequiredCredits();
        BigDecimal gradElective = plan == null || plan.getGradElectiveCredits() == null
                ? BigDecimal.ZERO : plan.getGradElectiveCredits();
        BigDecimal gradGenEdu = plan == null || plan.getGenEduCredits() == null
                ? BigDecimal.ZERO : plan.getGenEduCredits();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("studentId", studentId);
        m.put("major", st.getMajor());
        m.put("grade", st.getGrade());
        m.put("currentSelectedCredits", currentSelected);
        m.put("currentEarnedCredits", currentEarned);
        m.put("totalEarnedCredits", totalEarned);
        m.put("requiredEarnedCredits", required);
        m.put("electiveEarnedCredits", elective);
        m.put("genEduEarnedCredits", genEdu);
        m.put("failedCredits", failed);
        m.put("totalGpa", totalGpa);
        m.put("avgGpa", avgGpa);
        m.put("gradTotalCredits", gradTotal);
        m.put("gradRequiredCredits", gradRequired);
        m.put("gradElectiveCredits", gradElective);
        m.put("gradGenEduCredits", gradGenEdu);
        BigDecimal percent = gradTotal.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : totalEarned.multiply(BigDecimal.valueOf(100)).divide(gradTotal, 1, RoundingMode.HALF_UP);
        BigDecimal remaining = gradTotal.subtract(totalEarned).max(BigDecimal.ZERO);
        m.put("progressPercent", percent);
        m.put("remainingCredits", remaining);
        m.put("requiredPercent", gradRequired.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : required.multiply(BigDecimal.valueOf(100)).divide(gradRequired, 1, RoundingMode.HALF_UP));
        m.put("electivePercent", gradElective.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : elective.multiply(BigDecimal.valueOf(100)).divide(gradElective, 1, RoundingMode.HALF_UP));
        m.put("genEduPercent", gradGenEdu.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                : genEdu.multiply(BigDecimal.valueOf(100)).divide(gradGenEdu, 1, RoundingMode.HALF_UP));
        return m;
    }

    /** 教务端：按专业年级统计毕业学分进度 */
    public Map<String, Object> graduationSummary() {
        List<Student> students = studentMapper.selectList(null);
        Map<String, Map<String, Object>> group = new LinkedHashMap<>();
        for (Student st : students) {
            String key = (st.getMajor() == null ? "" : st.getMajor()) + "|" + (st.getGrade() == null ? "" : st.getGrade());
            Map<String, Object> g = group.computeIfAbsent(key, k -> {
                Map<String, Object> mm = new LinkedHashMap<>();
                mm.put("major", st.getMajor());
                mm.put("grade", st.getGrade());
                mm.put("studentCount", 0);
                mm.put("sumEarned", BigDecimal.ZERO);
                mm.put("minEarned", null);
                mm.put("maxEarned", null);
                mm.put("qualifiedCount", 0);
                mm.put("gradTotal", BigDecimal.ZERO);
                return mm;
            });
            BigDecimal earned = nz(st.getTotalEarnedCredits());
            g.put("studentCount", (Integer) g.get("studentCount") + 1);
            g.put("sumEarned", ((BigDecimal) g.get("sumEarned")).add(earned));
            if (g.get("minEarned") == null || earned.compareTo((BigDecimal) g.get("minEarned")) < 0)
                g.put("minEarned", earned);
            if (g.get("maxEarned") == null || earned.compareTo((BigDecimal) g.get("maxEarned")) > 0)
                g.put("maxEarned", earned);
            TrainingPlan plan = planMapper.selectOne(new QueryWrapper<TrainingPlan>()
                    .eq("major", st.getMajor() == null ? "" : st.getMajor())
                    .eq("grade", st.getGrade() == null ? "" : st.getGrade()).last("LIMIT 1"));
            BigDecimal gradTotal = plan == null || plan.getGradTotalCredits() == null
                    ? BigDecimal.ZERO : plan.getGradTotalCredits();
            g.put("gradTotal", gradTotal);
            if (gradTotal.compareTo(BigDecimal.ZERO) > 0 && earned.compareTo(gradTotal) >= 0)
                g.put("qualifiedCount", (Integer) g.get("qualifiedCount") + 1);
        }
        List<Map<String, Object>> items = new ArrayList<>();
        for (Map<String, Object> g : group.values()) {
            int n = (Integer) g.get("studentCount");
            BigDecimal avg = n == 0 ? BigDecimal.ZERO
                    : ((BigDecimal) g.get("sumEarned")).divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
            BigDecimal gradTotal = (BigDecimal) g.get("gradTotal");
            g.put("avgEarned", avg);
            g.put("avgPercent", gradTotal.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.ZERO
                    : avg.multiply(BigDecimal.valueOf(100)).divide(gradTotal, 1, RoundingMode.HALF_UP));
            g.put("qualifiedRate", n == 0 ? 0 : Math.round((Integer) g.get("qualifiedCount") * 100.0 / n));
            g.remove("sumEarned");
            items.add(g);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("total", items.size());
        return out;
    }

    private BigDecimal nz(BigDecimal v) { return v == null ? BigDecimal.ZERO : v; }
}
