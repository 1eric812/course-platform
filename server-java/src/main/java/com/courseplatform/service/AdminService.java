package com.courseplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.courseplatform.config.MetricsRegistry;
import com.courseplatform.config.ServerClock;
import com.courseplatform.entity.AnomalyTicket;
import com.courseplatform.entity.Course;
import com.courseplatform.entity.SelectionRule;
import com.courseplatform.entity.SelectionTicket;
import com.courseplatform.entity.Student;
import com.courseplatform.entity.StudentCourse;
import com.courseplatform.entity.SysUser;
import com.courseplatform.entity.Teacher;
import com.courseplatform.exception.ApiException;
import com.courseplatform.mapper.AnomalyTicketMapper;
import com.courseplatform.mapper.CourseMapper;
import com.courseplatform.mapper.SelectionRuleMapper;
import com.courseplatform.mapper.SelectionTicketMapper;
import com.courseplatform.mapper.StudentCourseMapper;
import com.courseplatform.mapper.StudentMapper;
import com.courseplatform.mapper.SysUserMapper;
import com.courseplatform.mapper.TeacherMapper;
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
    private final SemesterService semesterService;
    private final ScoreService scoreService;
    private final CreditsService creditsService;
    private final TrainingPlanService trainingPlanService;
    private final AnnouncementService announcementService;
    private final OperationLogService operationLogService;
    private final StudentMapper studentMapper;
    private final TeacherMapper teacherMapper;
    private final SysUserMapper sysUserMapper;

    public AdminService(CourseMapper courseMapper, SelectionRuleMapper ruleMapper, AnomalyTicketMapper anomalyMapper,
                        SelectionTicketMapper ticketMapper, StudentCourseMapper studentCourseMapper,
                        WishlistMapper wishlistMapper, ServerClock clock, MetricsRegistry metrics,
                        JdbcTemplate jdbcTemplate, SemesterService semesterService, ScoreService scoreService,
                        CreditsService creditsService, TrainingPlanService trainingPlanService,
                        AnnouncementService announcementService, OperationLogService operationLogService,
                        StudentMapper studentMapper, TeacherMapper teacherMapper, SysUserMapper sysUserMapper) {
        this.courseMapper = courseMapper;
        this.ruleMapper = ruleMapper;
        this.anomalyMapper = anomalyMapper;
        this.ticketMapper = ticketMapper;
        this.studentCourseMapper = studentCourseMapper;
        this.wishlistMapper = wishlistMapper;
        this.clock = clock;
        this.metrics = metrics;
        this.jdbcTemplate = jdbcTemplate;
        this.semesterService = semesterService;
        this.scoreService = scoreService;
        this.creditsService = creditsService;
        this.trainingPlanService = trainingPlanService;
        this.announcementService = announcementService;
        this.operationLogService = operationLogService;
        this.studentMapper = studentMapper;
        this.teacherMapper = teacherMapper;
        this.sysUserMapper = sysUserMapper;
    }

    private Map<String, Object> ruleMap(SelectionRule r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("selectionOpen", r.getSelectionOpen() != null && r.getSelectionOpen() == 1);
        m.put("creditLimit", r.getCreditLimit());
        m.put("maxWishlist", r.getMaxWishlist());
        m.put("allowCrossCampus", r.getAllowCrossCampus() != null && r.getAllowCrossCampus() == 1);
        m.put("blockOnConflict", r.getBlockOnConflict() != null && r.getBlockOnConflict() == 1);
        m.put("maxCourses", r.getMaxCourses());
        m.put("dropDeadline", r.getDropDeadline() == null ? null : r.getDropDeadline().toString().replace("T", " "));
        m.put("checkConflict", r.getCheckConflict() != null && r.getCheckConflict() == 1);
        m.put("checkPrereq", r.getCheckPrereq() != null && r.getCheckPrereq() == 1);
        m.put("checkGradeMajor", r.getCheckGradeMajor() != null && r.getCheckGradeMajor() == 1);
        m.put("allowRetake", r.getAllowRetake() != null && r.getAllowRetake() == 1);
        m.put("allowCrossMajor", r.getAllowCrossMajor() != null && r.getAllowCrossMajor() == 1);
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
        // —— 学业闭环新增选课规则字段 ——
        if (patch.get("maxCourses") != null) r.setMaxCourses(Integer.valueOf(String.valueOf(patch.get("maxCourses"))));
        if (patch.get("dropDeadline") != null) {
            Object v = patch.get("dropDeadline");
            if (v == null || String.valueOf(v).isBlank()) r.setDropDeadline(null);
            else r.setDropDeadline(LocalDateTime.parse(String.valueOf(v).replace(" ", "T")));
        }
        if (patch.get("checkConflict") != null) r.setCheckConflict(Boolean.TRUE.equals(patch.get("checkConflict")) ? 1 : 0);
        if (patch.get("checkPrereq") != null) r.setCheckPrereq(Boolean.TRUE.equals(patch.get("checkPrereq")) ? 1 : 0);
        if (patch.get("checkGradeMajor") != null) r.setCheckGradeMajor(Boolean.TRUE.equals(patch.get("checkGradeMajor")) ? 1 : 0);
        if (patch.get("allowRetake") != null) r.setAllowRetake(Boolean.TRUE.equals(patch.get("allowRetake")) ? 1 : 0);
        if (patch.get("allowCrossMajor") != null) r.setAllowCrossMajor(Boolean.TRUE.equals(patch.get("allowCrossMajor")) ? 1 : 0);
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

    /* ================= 学业闭环 ================= */

    public Map<String, Object> semesters() { return semesterService.listAll(); }

    public Map<String, Object> createSemester(Map<String, Object> body) { return semesterService.create(body); }

    public Map<String, Object> updateSemester(Long id, Map<String, Object> body) { return semesterService.update(id, body); }

    public Map<String, Object> archiveSemester(Long id) { return semesterService.archive(id); }

    public Map<String, Object> trainingPlans() { return trainingPlanService.listAll(); }

    public Map<String, Object> createTrainingPlan(Map<String, Object> body) { return trainingPlanService.create(body); }

    public Map<String, Object> updateTrainingPlan(Long id, Map<String, Object> body) { return trainingPlanService.update(id, body); }

    /** 全部课程（含学期与状态），可按 semesterId / category 过滤 */
    public Map<String, Object> allCourses(Long semesterId, String category) {
        QueryWrapper<Course> qw = new QueryWrapper<>();
        if (semesterId != null) qw.eq("semester_id", semesterId);
        if (category != null && !category.isBlank()) qw.eq("category", category);
        qw.orderByAsc("code");
        List<Course> list = courseMapper.selectList(qw);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", list);
        out.put("total", list.size());
        return out;
    }

    @Transactional
    public Map<String, Object> createCourse(Map<String, Object> body) {
        Course c = new Course();
        c.setCode(str(body, "code"));
        c.setName(str(body, "name"));
        c.setCategory(str(body, "category"));
        c.setCredits(dec(body, "credits", 2.0));
        c.setTeacherName(str(body, "teacherName"));
        if (body.get("teacherId") != null) {
            Long tid = Long.valueOf(String.valueOf(body.get("teacherId")));
            Teacher t = teacherMapper.selectById(tid);
            if (t == null) throw new ApiException(400, "授课教师不存在", null);
            c.setTeacherId(tid);
            if (c.getTeacherName() == null || c.getTeacherName().isBlank()) {
                SysUser su = sysUserMapper.selectById(t.getUserId());
                c.setTeacherName(su == null ? "" : su.getRealName());
            }
        }
        c.setCampus(str(body, "campus"));
        c.setPlace(str(body, "place"));
        c.setAssessment(str(body, "assessment"));
        c.setCapacity(intOf(body, "capacity", 60));
        c.setEnrolled(0);
        c.setRating(decOrNull(body, "rating"));
        c.setPrereqName(str(body, "prereqName"));
        c.setTags(str(body, "tags"));
        c.setIntro(str(body, "intro"));
        // 学业闭环扩展字段
        c.setCourseAttr(str(body, "courseAttr"));
        c.setTotalHours(intOfNullable(body, "totalHours"));
        c.setTheoryHours(intOfNullable(body, "theoryHours"));
        c.setPracticeHours(intOfNullable(body, "practiceHours"));
        c.setLabHours(intOfNullable(body, "labHours"));
        c.setSemesterId(longOfNullable(body, "semesterId"));
        c.setWeeks(str(body, "weeks"));
        c.setLimitGrade(str(body, "limitGrade"));
        c.setLimitMajor(str(body, "limitMajor"));
        c.setLimitClass(str(body, "limitClass"));
        c.setPrereqRequired(booleanInt(body, "prereqRequired"));
        c.setAllowRetake(booleanInt(body, "allowRetake"));
        c.setAllowCrossMajor(booleanInt(body, "allowCrossMajor"));
        c.setSyllabus(str(body, "syllabus"));
        c.setAssessmentDetail(str(body, "assessmentDetail"));
        c.setOpenCollege(str(body, "openCollege"));
        c.setApplicableMajor(str(body, "applicableMajor"));
        c.setPrereqRequirement(str(body, "prereqRequirement"));
        c.setCourseObjective(str(body, "courseObjective"));
        c.setTextbook(str(body, "textbook"));
        c.setCourseStatus(str(body, "courseStatus"));
        courseMapper.insert(c);
        operationLogService.write(null, "教务管理员", "创建课程", "course:" + c.getId(), c.getName() + "(" + c.getCode() + ")");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("course", c);
        return out;
    }

    @Transactional
    public Map<String, Object> updateCourse(Long id, Map<String, Object> body) {
        Course c = courseMapper.selectById(id);
        if (c == null) throw new ApiException(404, "课程不存在", null);
        copyStr(body, "name", c::setName);
        copyStr(body, "code", c::setCode);
        copyStr(body, "category", c::setCategory);
        if (body.get("credits") != null) c.setCredits(dec(body, "credits", null));
        if (body.get("teacherId") != null) {
            Long tid = Long.valueOf(String.valueOf(body.get("teacherId")));
            Teacher t = teacherMapper.selectById(tid);
            if (t == null) throw new ApiException(400, "授课教师不存在", null);
            c.setTeacherId(tid);
            SysUser su = sysUserMapper.selectById(t.getUserId());
            c.setTeacherName(su == null ? "" : su.getRealName());
        }
        copyStr(body, "teacherName", c::setTeacherName);
        copyStr(body, "campus", c::setCampus);
        copyStr(body, "place", c::setPlace);
        copyStr(body, "assessment", c::setAssessment);
        if (body.get("capacity") != null) c.setCapacity(intOf(body, "capacity", null));
        if (body.get("rating") != null) c.setRating(decOrNull(body, "rating"));
        copyStr(body, "prereqName", c::setPrereqName);
        copyStr(body, "tags", c::setTags);
        copyStr(body, "intro", c::setIntro);
        copyStr(body, "courseAttr", c::setCourseAttr);
        if (body.get("totalHours") != null) c.setTotalHours(intOfNullable(body, "totalHours"));
        if (body.get("theoryHours") != null) c.setTheoryHours(intOfNullable(body, "theoryHours"));
        if (body.get("practiceHours") != null) c.setPracticeHours(intOfNullable(body, "practiceHours"));
        if (body.get("labHours") != null) c.setLabHours(intOfNullable(body, "labHours"));
        if (body.get("semesterId") != null) c.setSemesterId(longOfNullable(body, "semesterId"));
        copyStr(body, "weeks", c::setWeeks);
        copyStr(body, "limitGrade", c::setLimitGrade);
        copyStr(body, "limitMajor", c::setLimitMajor);
        copyStr(body, "limitClass", c::setLimitClass);
        if (body.get("prereqRequired") != null) c.setPrereqRequired(booleanInt(body, "prereqRequired"));
        if (body.get("allowRetake") != null) c.setAllowRetake(booleanInt(body, "allowRetake"));
        if (body.get("allowCrossMajor") != null) c.setAllowCrossMajor(booleanInt(body, "allowCrossMajor"));
        copyStr(body, "syllabus", c::setSyllabus);
        copyStr(body, "assessmentDetail", c::setAssessmentDetail);
        copyStr(body, "openCollege", c::setOpenCollege);
        copyStr(body, "applicableMajor", c::setApplicableMajor);
        copyStr(body, "prereqRequirement", c::setPrereqRequirement);
        copyStr(body, "courseObjective", c::setCourseObjective);
        copyStr(body, "textbook", c::setTextbook);
        copyStr(body, "courseStatus", c::setCourseStatus);
        courseMapper.updateById(c);
        operationLogService.write(null, "教务管理员", "更新课程", "course:" + c.getId(), c.getName());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("course", c);
        return out;
    }

    public Map<String, Object> scores(Long semesterId, String status) { return scoreService.listByStatus(semesterId, status); }

    public Map<String, Object> auditScore(Long id, String action) { return scoreService.auditScore(id, action); }

    public Map<String, Object> creditsSettle(Long semesterId) { return creditsService.settleSemester(semesterId); }

    public Map<String, Object> graduation() { return creditsService.graduationSummary(); }

    public Map<String, Object> announcements() { return announcementService.adminList(); }

    public Map<String, Object> createAnnouncement(Map<String, Object> body) { return announcementService.create(body); }

    public Map<String, Object> updateAnnouncement(Long id, Map<String, Object> body) { return announcementService.update(id, body); }

    public Map<String, Object> logs(Integer page, Integer size) { return operationLogService.list(page, size); }

    /* ---------- 小工具 ---------- */

    private String str(Map<String, Object> m, String key) {
        Object v = m.get(key);
        return v == null ? null : String.valueOf(v).trim();
    }

    private BigDecimal dec(Map<String, Object> m, String key, Double dft) {
        Object v = m.get(key);
        if (v == null || String.valueOf(v).isBlank()) return dft == null ? null : BigDecimal.valueOf(dft);
        return new BigDecimal(String.valueOf(v));
    }

    private BigDecimal decOrNull(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null || String.valueOf(v).isBlank()) return null;
        return new BigDecimal(String.valueOf(v));
    }

    private int intOf(Map<String, Object> m, String key, Integer dft) {
        Object v = m.get(key);
        if (v == null || String.valueOf(v).isBlank()) return dft == null ? 0 : dft;
        return Integer.parseInt(String.valueOf(v));
    }

    private Integer intOfNullable(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null || String.valueOf(v).isBlank()) return null;
        return Integer.valueOf(String.valueOf(v));
    }

    private Long longOfNullable(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null || String.valueOf(v).isBlank()) return null;
        return Long.valueOf(String.valueOf(v));
    }

    private Integer booleanInt(Map<String, Object> m, String key) {
        Object v = m.get(key);
        if (v == null) return 0;
        if (v instanceof Boolean) return (Boolean) v ? 1 : 0;
        return Boolean.parseBoolean(String.valueOf(v)) || "1".equals(String.valueOf(v)) ? 1 : 0;
    }

    private void copyStr(Map<String, Object> m, String key, java.util.function.Consumer<String> setter) {
        Object v = m.get(key);
        if (v != null) setter.accept(String.valueOf(v).trim());
    }
}
