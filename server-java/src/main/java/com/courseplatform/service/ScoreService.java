package com.courseplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.courseplatform.entity.Course;
import com.courseplatform.entity.Score;
import com.courseplatform.entity.Student;
import com.courseplatform.entity.StudentCourse;
import com.courseplatform.entity.SysUser;
import com.courseplatform.entity.Teacher;
import com.courseplatform.exception.ApiException;
import com.courseplatform.mapper.CourseMapper;
import com.courseplatform.mapper.ScoreMapper;
import com.courseplatform.mapper.StudentCourseMapper;
import com.courseplatform.mapper.StudentMapper;
import com.courseplatform.mapper.SysUserMapper;
import com.courseplatform.mapper.TeacherMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 成绩体系：录入 / 审核 / 查询 / 统计 / 挂科重修名单 */
@Service
public class ScoreService {

    private final ScoreMapper scoreMapper;
    private final CourseMapper courseMapper;
    private final StudentCourseMapper studentCourseMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper sysUserMapper;
    private final TeacherMapper teacherMapper;
    private final SemesterService semesterService;
    private final OperationLogService operationLogService;

    public ScoreService(ScoreMapper scoreMapper, CourseMapper courseMapper, StudentCourseMapper studentCourseMapper,
                        StudentMapper studentMapper, SysUserMapper sysUserMapper, TeacherMapper teacherMapper,
                        SemesterService semesterService, OperationLogService operationLogService) {
        this.scoreMapper = scoreMapper;
        this.courseMapper = courseMapper;
        this.studentCourseMapper = studentCourseMapper;
        this.studentMapper = studentMapper;
        this.sysUserMapper = sysUserMapper;
        this.teacherMapper = teacherMapper;
        this.semesterService = semesterService;
        this.operationLogService = operationLogService;
    }

    private Map<String, Object> scoreMap(Score sc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", sc.getId());
        m.put("studentId", sc.getStudentId());
        m.put("courseId", sc.getCourseId());
        m.put("semesterId", sc.getSemesterId());
        m.put("regularScore", sc.getRegularScore());
        m.put("attendanceScore", sc.getAttendanceScore());
        m.put("homeworkScore", sc.getHomeworkScore());
        m.put("midtermScore", sc.getMidtermScore());
        m.put("finalScore", sc.getFinalScore());
        m.put("totalScore", sc.getTotalScore());
        m.put("gradeLevel", sc.getGradeLevel());
        m.put("courseGpa", sc.getCourseGpa());
        m.put("creditObtained", sc.getCreditObtained() != null && sc.getCreditObtained() == 1);
        m.put("retakeFlag", sc.getRetakeFlag() != null && sc.getRetakeFlag() == 1);
        m.put("retakeScore", sc.getRetakeScore());
        m.put("retakeCourseFlag", sc.getRetakeCourseFlag() != null && sc.getRetakeCourseFlag() == 1);
        m.put("retakeTimes", sc.getRetakeTimes());
        m.put("status", sc.getStatus());
        return m;
    }

    private Map<String, Object> courseMap(Course c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("code", c.getCode());
        m.put("name", c.getName());
        m.put("category", c.getCategory());
        m.put("credits", c.getCredits());
        m.put("teacherName", c.getTeacherName());
        m.put("semesterId", c.getSemesterId());
        return m;
    }

    private StudentCourse findEnrollment(Long studentId, Long courseId) {
        return studentCourseMapper.selectOne(new QueryWrapper<StudentCourse>()
                .eq("student_id", studentId).eq("course_id", courseId).last("LIMIT 1"));
    }

    /** 学生某学期（可空=当前学期）成绩单：分项/总评/等级/绩点/补考/重修/状态 */
    public Map<String, Object> studentScores(Long studentId, Long semesterId) {
        if (semesterId == null) {
            var s = semesterService.getCurrent();
            semesterId = s == null ? null : s.getId();
        }
        QueryWrapper<Score> qw = new QueryWrapper<Score>().eq("student_id", studentId).orderByAsc("course_id");
        if (semesterId != null) qw.eq("semester_id", semesterId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Score sc : scoreMapper.selectList(qw)) {
            Map<String, Object> m = scoreMap(sc);
            Course c = courseMapper.selectById(sc.getCourseId());
            if (c != null) m.putAll(courseMap(c));
            items.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("semesterId", semesterId);
        out.put("items", items);
        out.put("total", items.size());
        return out;
    }

    /** 学生历年成绩（已归档），按学期分组 */
    public Map<String, Object> studentScoreHistory(Long studentId) {
        List<Score> list = scoreMapper.selectList(new QueryWrapper<Score>()
                .eq("student_id", studentId)
                .orderByAsc("semester_id").orderByAsc("course_id"));
        Map<Long, Map<String, Object>> bySemester = new LinkedHashMap<>();
        for (Score sc : list) {
            Long sid = sc.getSemesterId();
            Map<String, Object> g = bySemester.computeIfAbsent(sid, k -> {
                Map<String, Object> mm = new LinkedHashMap<>();
                mm.put("semesterId", k);
                mm.put("semesterName", k == null ? "" : String.valueOf(k));
                mm.put("items", new ArrayList<Map<String, Object>>());
                mm.put("courseCount", 0);
                mm.put("totalCredits", BigDecimal.ZERO);
                mm.put("avgGpa", BigDecimal.ZERO);
                return mm;
            });
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> its = (List<Map<String, Object>>) g.get("items");
            Map<String, Object> m = scoreMap(sc);
            Course c = courseMapper.selectById(sc.getCourseId());
            if (c != null) m.putAll(courseMap(c));
            its.add(m);
            g.put("courseCount", its.size());
            BigDecimal credits = c == null || c.getCredits() == null ? BigDecimal.ZERO : c.getCredits();
            g.put("totalCredits", ((BigDecimal) g.get("totalCredits")).add(credits));
            BigDecimal gpa = sc.getCourseGpa() == null ? BigDecimal.ZERO : sc.getCourseGpa();
            g.put("avgGpa", ((BigDecimal) g.get("avgGpa")).add(gpa));
        }
        List<Map<String, Object>> groups = new ArrayList<>(bySemester.values());
        for (Map<String, Object> g : groups) {
            int n = ((List<?>) g.get("items")).size();
            g.put("avgGpa", n == 0 ? BigDecimal.ZERO : ((BigDecimal) g.get("avgGpa"))
                    .divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP));
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("semesters", groups);
        out.put("total", groups.size());
        return out;
    }

    /** 校验教师身份是否为该课程授课教师（teacher_id 或 teacher_name 匹配） */
    private boolean isCourseOwner(Long teacherId, Course c) {
        if (c.getTeacherId() != null && c.getTeacherId().equals(teacherId)) return true;
        Teacher t = teacherMapper.selectById(teacherId);
        if (t == null) return false;
        SysUser u = sysUserMapper.selectById(t.getUserId());
        return u != null && c.getTeacherName() != null && c.getTeacherName().equals(u.getRealName());
    }

    /** 教师查看某课程选课学生名单 + 成绩（未录入为 null） */
    public Map<String, Object> teacherCourseScores(Long teacherId, Long courseId) {
        Course c = courseMapper.selectById(courseId);
        if (c == null) throw new ApiException(404, "课程不存在", null);
        if (!isCourseOwner(teacherId, c)) throw new ApiException(403, "您不是该课程授课教师", null);
        List<StudentCourse> enrolls = studentCourseMapper.selectList(new QueryWrapper<StudentCourse>()
                .eq("course_id", courseId).ne("status", "DROPPED").orderByAsc("student_id"));
        List<Map<String, Object>> items = new ArrayList<>();
        for (StudentCourse ec : enrolls) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("studentId", ec.getStudentId());
            m.put("status", ec.getStatus());
            Student st = studentMapper.selectById(ec.getStudentId());
            if (st != null) {
                m.put("studentNo", st.getStudentNo());
                m.put("major", st.getMajor());
                m.put("grade", st.getGrade());
                m.put("className", st.getClassName());
                SysUser su = sysUserMapper.selectById(st.getUserId());
                m.put("name", su == null ? "" : su.getRealName());
            }
            Score sc = scoreMapper.selectOne(new QueryWrapper<Score>()
                    .eq("student_id", ec.getStudentId()).eq("course_id", courseId)
                    .eq("semester_id", c.getSemesterId()).last("LIMIT 1"));
            m.put("score", sc == null ? null : scoreMap(sc));
            items.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("course", courseMap(c));
        out.put("items", items);
        out.put("total", items.size());
        return out;
    }

    /** 教师批量保存成绩：自动计算总评/等级/绩点/学分获得，状态='已录入' */
    @Transactional
    public Map<String, Object> teacherSaveScores(Long teacherId, Long courseId, List<Map<String, Object>> items) {
        if (items == null || items.isEmpty()) throw new ApiException(400, "成绩列表不能为空", null);
        Course c = courseMapper.selectById(courseId);
        if (c == null) throw new ApiException(404, "课程不存在", null);
        if (!isCourseOwner(teacherId, c)) throw new ApiException(403, "您不是该课程授课教师", null);
        Long semesterId = c.getSemesterId();
        if (semesterId == null) {
            var s = semesterService.getCurrent();
            semesterId = s == null ? null : s.getId();
        }
        int saved = 0;
        for (Map<String, Object> item : items) {
            if (item.get("studentId") == null) throw new ApiException(400, "成绩条目缺少 studentId", null);
            Long studentId = Long.valueOf(String.valueOf(item.get("studentId")));
            StudentCourse ec = findEnrollment(studentId, courseId);
            if (ec == null || "DROPPED".equals(ec.getStatus()))
                throw new ApiException(400, "学生（id=" + studentId + "）未选修该课程，无法录入成绩", null);
            BigDecimal regular = decimal(item.get("regularScore"));
            BigDecimal attendance = decimal(item.get("attendanceScore"));
            BigDecimal homework = decimal(item.get("homeworkScore"));
            BigDecimal midterm = decimal(item.get("midtermScore"));
            BigDecimal fin = decimal(item.get("finalScore"));
            BigDecimal total = computeTotal(regular, attendance, homework, midterm, fin);
            Score sc = scoreMapper.selectOne(new QueryWrapper<Score>()
                    .eq("student_id", studentId).eq("course_id", courseId)
                    .eq("semester_id", semesterId).last("LIMIT 1"));
            if (sc == null) {
                sc = new Score();
                sc.setStudentId(studentId);
                sc.setCourseId(courseId);
                sc.setSemesterId(semesterId);
                sc.setStatus("已录入");
                sc.setCreatedAt(LocalDateTime.now());
            }
            if (regular != null) sc.setRegularScore(regular);
            if (attendance != null) sc.setAttendanceScore(attendance);
            if (homework != null) sc.setHomeworkScore(homework);
            if (midterm != null) sc.setMidtermScore(midterm);
            if (fin != null) sc.setFinalScore(fin);
            sc.setTotalScore(total);
            sc.setGradeLevel(gradeLevel(total));
            sc.setCourseGpa(courseGpa(total));
            sc.setCreditObtained("不及格".equals(sc.getGradeLevel()) ? 0 : 1);
            sc.setUpdatedAt(LocalDateTime.now());
            if (sc.getId() == null) scoreMapper.insert(sc); else scoreMapper.updateById(sc);
            saved++;
        }
        operationLogService.write(42L, "admin", "成绩录入", c.getName(), "教师(id=" + teacherId + ")录入课程成绩 " + saved + " 条");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("saved", saved);
        out.put("courseId", courseId);
        return out;
    }

    /** 教务审核成绩：通过→已审核（写日志）；驳回→待录入 */
    @Transactional
    public Map<String, Object> auditScore(Long id, String action) {
        Score sc = scoreMapper.selectById(id);
        if (sc == null) throw new ApiException(404, "成绩记录不存在", null);
        if ("APPROVE".equalsIgnoreCase(action)) {
            sc.setStatus("已审核");
            sc.setUpdatedAt(LocalDateTime.now());
            scoreMapper.updateById(sc);
            Course c = courseMapper.selectById(sc.getCourseId());
            operationLogService.write(42L, "admin", "成绩审核", c == null ? "" : c.getName(),
                    "通过成绩记录（id=" + id + "，studentId=" + sc.getStudentId() + "）");
        } else if ("REJECT".equalsIgnoreCase(action)) {
            sc.setStatus("待录入");
            sc.setUpdatedAt(LocalDateTime.now());
            scoreMapper.updateById(sc);
            Course c = courseMapper.selectById(sc.getCourseId());
            operationLogService.write(42L, "admin", "成绩审核", c == null ? "" : c.getName(),
                    "驳回成绩记录（id=" + id + "，studentId=" + sc.getStudentId() + "）");
        } else {
            throw new ApiException(400, "未知审核操作：" + action, null);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("status", sc.getStatus());
        return out;
    }

    /** 教务查看全部成绩（可按学期 / 状态过滤） */
    public Map<String, Object> listByStatus(Long semesterId, String status) {
        QueryWrapper<Score> qw = new QueryWrapper<Score>().orderByDesc("id");
        if (semesterId != null) qw.eq("semester_id", semesterId);
        if (status != null && !status.isBlank()) qw.eq("status", status);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Score sc : scoreMapper.selectList(qw)) {
            Map<String, Object> m = scoreMap(sc);
            Course c = courseMapper.selectById(sc.getCourseId());
            if (c != null) m.putAll(courseMap(c));
            Student st = studentMapper.selectById(sc.getStudentId());
            if (st != null) {
                m.put("studentNo", st.getStudentNo());
                SysUser su = sysUserMapper.selectById(st.getUserId());
                m.put("studentName", su == null ? "" : su.getRealName());
            }
            items.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("total", items.size());
        return out;
    }

    /** 课程成绩统计：最高/最低/平均分、通过率、挂科率、等级分布 */
    public Map<String, Object> courseStats(Long courseId) {
        Course c = courseMapper.selectById(courseId);
        if (c == null) throw new ApiException(404, "课程不存在", null);
        List<Score> list = scoreMapper.selectList(new QueryWrapper<Score>()
                .eq("course_id", courseId).isNotNull("total_score"));
        int n = list.size();
        BigDecimal max = null, min = null, sum = BigDecimal.ZERO;
        int pass = 0, you = 0, liang = 0, zhong = 0, jige = 0, bujige = 0;
        for (Score sc : list) {
            BigDecimal t = sc.getTotalScore();
            if (max == null || t.compareTo(max) > 0) max = t;
            if (min == null || t.compareTo(min) < 0) min = t;
            sum = sum.add(t);
            if (t.compareTo(BigDecimal.valueOf(60)) >= 0) pass++;
            switch (sc.getGradeLevel() == null ? "" : sc.getGradeLevel()) {
                case "优" -> you++;
                case "良" -> liang++;
                case "中" -> zhong++;
                case "及格" -> jige++;
                default -> bujige++;
            }
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("courseId", courseId);
        m.put("courseName", c.getName());
        m.put("count", n);
        m.put("maxScore", max);
        m.put("minScore", min);
        m.put("avgScore", n == 0 ? null : sum.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP));
        m.put("passRate", n == 0 ? 0 : Math.round(pass * 100.0 / n));
        m.put("failRate", n == 0 ? 0 : Math.round((n - pass) * 100.0 / n));
        Map<String, Object> dist = new LinkedHashMap<>();
        dist.put("优", you);
        dist.put("良", liang);
        dist.put("中", zhong);
        dist.put("及格", jige);
        dist.put("不及格", bujige);
        m.put("distribution", dist);
        return m;
    }

    /** 挂科/重修学生名单 */
    public Map<String, Object> failList(Long courseId) {
        Course c = courseMapper.selectById(courseId);
        if (c == null) throw new ApiException(404, "课程不存在", null);
        List<Score> list = scoreMapper.selectList(new QueryWrapper<Score>()
                .eq("course_id", courseId)
                .and(w -> w.eq("grade_level", "不及格")
                        .or().eq("credit_obtained", 0)
                        .or().eq("retake_course_flag", 1)
                        .or().eq("retake_flag", 1)));
        List<Map<String, Object>> items = new ArrayList<>();
        for (Score sc : list) {
            Map<String, Object> m = scoreMap(sc);
            m.put("courseId", courseId);
            m.put("courseName", c.getName());
            Student st = studentMapper.selectById(sc.getStudentId());
            if (st != null) {
                m.put("studentNo", st.getStudentNo());
                m.put("major", st.getMajor());
                m.put("grade", st.getGrade());
                SysUser su = sysUserMapper.selectById(st.getUserId());
                m.put("studentName", su == null ? "" : su.getRealName());
            }
            items.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("course", courseMap(c));
        out.put("items", items);
        out.put("total", items.size());
        return out;
    }

    // ---- 计算工具 ----

    private BigDecimal computeTotal(BigDecimal regular, BigDecimal attendance, BigDecimal homework,
                                    BigDecimal midterm, BigDecimal fin) {
        double r = nz(regular), a = nz(attendance), h = nz(homework), f = nz(fin);
        double raw = f > 0 ? r * 0.3 + f * 0.7 : (r + a + h) / 3.0;
        return BigDecimal.valueOf(Math.floor(raw)).setScale(1, RoundingMode.HALF_UP);
    }

    private String gradeLevel(BigDecimal total) {
        int t = total == null ? 0 : total.intValue();
        if (t >= 90) return "优";
        if (t >= 80) return "良";
        if (t >= 70) return "中";
        if (t >= 60) return "及格";
        return "不及格";
    }

    private BigDecimal courseGpa(BigDecimal total) {
        int t = total == null ? 0 : total.intValue();
        if (t >= 90) return new BigDecimal("4.0");
        if (t >= 80) return new BigDecimal("3.0");
        if (t >= 70) return new BigDecimal("2.0");
        if (t >= 60) return new BigDecimal("1.0");
        return new BigDecimal("0.0");
    }

    private double nz(BigDecimal v) { return v == null ? 0 : v.doubleValue(); }

    private BigDecimal decimal(Object v) {
        if (v == null || String.valueOf(v).isBlank()) return null;
        return new BigDecimal(String.valueOf(v));
    }
}
