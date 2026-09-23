package com.courseplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.courseplatform.entity.Course;
import com.courseplatform.entity.CourseSchedule;
import com.courseplatform.entity.Message;
import com.courseplatform.entity.SeatSubscription;
import com.courseplatform.entity.SelectionPeriod;
import com.courseplatform.entity.SelectionRule;
import com.courseplatform.entity.SelectionTicket;
import com.courseplatform.entity.SelectionTicketItem;
import com.courseplatform.entity.Semester;
import com.courseplatform.entity.Student;
import com.courseplatform.entity.StudentCourse;
import com.courseplatform.entity.SysUser;
import com.courseplatform.entity.UserPreference;
import com.courseplatform.entity.Wishlist;
import com.courseplatform.exception.ApiException;
import com.courseplatform.mapper.CourseMapper;
import com.courseplatform.mapper.CourseScheduleMapper;
import com.courseplatform.mapper.MessageMapper;
import com.courseplatform.mapper.SemesterMapper;
import com.courseplatform.mapper.StudentMapper;
import com.courseplatform.mapper.SysUserMapper;
import com.courseplatform.mapper.SeatSubscriptionMapper;
import com.courseplatform.mapper.SelectionRuleMapper;
import com.courseplatform.mapper.SelectionTicketItemMapper;
import com.courseplatform.mapper.SelectionTicketMapper;
import com.courseplatform.mapper.StudentCourseMapper;
import com.courseplatform.mapper.UserPreferenceMapper;
import com.courseplatform.mapper.WishlistMapper;
import com.courseplatform.security.LoginUser;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/** 学生端：心愿单 / 选课受理 / 课表 / 消息 / 偏好 / 放号订阅 */
@Service
public class StudentService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final CourseService courseService;
    private final CourseMapper courseMapper;
    private final WishlistMapper wishlistMapper;
    private final StudentCourseMapper studentCourseMapper;
    private final SeatSubscriptionMapper subscriptionMapper;
    private final MessageMapper messageMapper;
    private final UserPreferenceMapper prefMapper;
    private final SelectionRuleMapper ruleMapper;
    private final SelectionTicketMapper ticketMapper;
    private final SelectionTicketItemMapper ticketItemMapper;
    private final ThreadPoolTaskExecutor executor;
    private final PeriodService periodService;
    private final ScoreService scoreService;
    private final CreditsService creditsService;
    private final AnnouncementService announcementService;
    private final SemesterMapper semesterMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper sysUserMapper;
    private final CourseScheduleMapper scheduleMapper;

    public StudentService(CourseService courseService, CourseMapper courseMapper, WishlistMapper wishlistMapper,
                          StudentCourseMapper studentCourseMapper, SeatSubscriptionMapper subscriptionMapper,
                          MessageMapper messageMapper, UserPreferenceMapper prefMapper, SelectionRuleMapper ruleMapper,
                          SelectionTicketMapper ticketMapper, SelectionTicketItemMapper ticketItemMapper,
                          PeriodService periodService, ScoreService scoreService, CreditsService creditsService,
                          AnnouncementService announcementService, SemesterMapper semesterMapper,
                          StudentMapper studentMapper, SysUserMapper sysUserMapper, CourseScheduleMapper scheduleMapper) {
        this.courseService = courseService;
        this.courseMapper = courseMapper;
        this.wishlistMapper = wishlistMapper;
        this.studentCourseMapper = studentCourseMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.messageMapper = messageMapper;
        this.prefMapper = prefMapper;
        this.ruleMapper = ruleMapper;
        this.ticketMapper = ticketMapper;
        this.ticketItemMapper = ticketItemMapper;
        this.periodService = periodService;
        this.scoreService = scoreService;
        this.creditsService = creditsService;
        this.announcementService = announcementService;
        this.semesterMapper = semesterMapper;
        this.studentMapper = studentMapper;
        this.sysUserMapper = sysUserMapper;
        this.scheduleMapper = scheduleMapper;
        this.executor = new ThreadPoolTaskExecutor();
        this.executor.setCorePoolSize(2);
        this.executor.setMaxPoolSize(4);
        this.executor.setQueueCapacity(50);
        this.executor.initialize();
    }

    private List<Course> enrolledCourses(Long sid) {
        if (sid == null) return new ArrayList<>();
        List<Long> ids = studentCourseMapper.selectList(new QueryWrapper<StudentCourse>()
                .eq("student_id", sid).eq("status", "SELECTED")).stream()
                .map(StudentCourse::getCourseId).collect(Collectors.toList());
        if (ids.isEmpty()) return new ArrayList<>();
        return courseMapper.selectBatchIds(ids);
    }

    private BigDecimal totalCredits(Long sid) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Course c : enrolledCourses(sid)) if (c.getCredits() != null) sum = sum.add(c.getCredits());
        return sum;
    }

    /* ================= 心愿单 ================= */

    public Map<String, Object> wishlist(LoginUser u) {
        List<Course> enrolled = enrolledCourses(u.getStudentId());
        List<Map<String, Object>> items = wishlistMapper.selectList(new QueryWrapper<Wishlist>()
                        .eq("student_id", u.getStudentId()).orderByAsc("priority"))
                .stream().map(w -> {
                    Course c = courseMapper.selectById(w.getCourseId());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("courseId", w.getCourseId());
                    m.put("priority", w.getPriority());
                    m.put("course", c == null ? null : courseService.enrich(c));
                    List<Map<String, Object>> cd = c == null ? new ArrayList<>() : courseService.detectConflicts(c, enrolled, courseService.rule());
                    m.put("conflict", cd);
                    m.put("blocked", cd.stream().anyMatch(x -> "hard".equals(x.get("level"))));
                    return m;
                }).collect(Collectors.toList());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        return out;
    }

    @Transactional
    public Map<String, Object> wishlistAdd(Long courseId, LoginUser u) {
        Course c = courseMapper.selectById(courseId);
        if (c == null) throw new ApiException(400, "课程不存在", null);
        Long sid = u.getStudentId();
        if (studentCourseMapper.selectCount(new QueryWrapper<StudentCourse>()
                .eq("student_id", sid).eq("course_id", courseId).eq("status", "SELECTED")) > 0)
            throw new ApiException(400, "该课程已选中，无需加入心愿单", null);
        if (wishlistMapper.selectCount(new QueryWrapper<Wishlist>()
                .eq("student_id", sid).eq("course_id", courseId)) > 0)
            throw new ApiException(400, "已在心愿单中", null);
        SelectionRule rule = courseService.rule();
        int max = rule.getMaxWishlist() == null ? 8 : rule.getMaxWishlist();
        long count = wishlistMapper.selectCount(new QueryWrapper<Wishlist>().eq("student_id", sid));
        if (count >= max) throw new ApiException(400, "心愿单已达上限（" + max + " 门），请先移除部分课程", null);
        Wishlist w = new Wishlist();
        w.setStudentId(sid);
        w.setCourseId(courseId);
        int top = wishlistMapper.selectList(new QueryWrapper<Wishlist>().eq("student_id", sid)).stream()
                .mapToInt(Wishlist::getPriority).max().orElse(0);
        w.setPriority(top + 1);
        w.setCreatedAt(LocalDateTime.now());
        wishlistMapper.insert(w);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        return out;
    }

    @Transactional
    public Map<String, Object> wishlistRemove(Long courseId, LoginUser u) {
        wishlistMapper.delete(new QueryWrapper<Wishlist>()
                .eq("student_id", u.getStudentId()).eq("course_id", courseId));
        // 重新排序
        List<Wishlist> list = wishlistMapper.selectList(new QueryWrapper<Wishlist>()
                .eq("student_id", u.getStudentId()).orderByAsc("priority"));
        for (int i = 0; i < list.size(); i++) {
            Wishlist w = list.get(i);
            w.setPriority(i + 1);
            wishlistMapper.updateById(w);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        return out;
    }

    @Transactional
    public Map<String, Object> wishlistReorder(List<Long> orderedIds, LoginUser u) {
        if (orderedIds == null) throw new ApiException(400, "参数错误", null);
        Map<Long, Wishlist> map = wishlistMapper.selectList(new QueryWrapper<Wishlist>().eq("student_id", u.getStudentId()))
                .stream().collect(Collectors.toMap(Wishlist::getCourseId, w -> w, (a, b) -> a));
        List<Wishlist> next = new ArrayList<>();
        for (int i = 0; i < orderedIds.size(); i++) {
            Wishlist w = map.get(orderedIds.get(i));
            if (w != null) { w.setPriority(i + 1); next.add(w); }
        }
        for (Wishlist w : next) wishlistMapper.updateById(w);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        return out;
    }

    /* ================= 选课受理（异步） ================= */

    @Transactional
    public Map<String, Object> submitSelection(LoginUser u) {
        List<Wishlist> wish = wishlistMapper.selectList(new QueryWrapper<Wishlist>()
                .eq("student_id", u.getStudentId()).orderByAsc("priority"));
        if (wish.isEmpty()) throw new ApiException(400, "心愿单为空，请先添加课程", null);
        SelectionRule rule = courseService.rule();
        if (rule.getSelectionOpen() == null || rule.getSelectionOpen() != 1)
            throw new ApiException(400, "选课未开放（教务已暂时关闭选课通道）", null);
        /* 阶段窗口门禁：教务开关之外，还要求当前时刻落在某个 selection_period 阶段内。
         * 与静态版 docs/js/backend/service.js#submitSelection 保持同一套判断口径。 */
        if (periodService.current() == null) {
            SelectionPeriod nxt = periodService.next();
            String msg = nxt != null
                    ? "当前不在选课阶段内，「" + nxt.getPhaseName() + "」将于 "
                      + nxt.getStartTime().toLocalDate() + " " + nxt.getStartTime().toLocalTime() + " 开放"
                    : "当前不在选课阶段内，请等待教务安排选课时间";
            throw new ApiException(400, msg, null);
        }

        long processing = ticketMapper.selectCount(new QueryWrapper<SelectionTicket>()
                .eq("student_id", u.getStudentId())
                .in("status", "ACCEPTED", "PROCESSING"));
        String ticketNo = "T-" + System.currentTimeMillis();
        SelectionTicket t = new SelectionTicket();
        t.setTicketNo(ticketNo);
        t.setStudentId(u.getStudentId());
        t.setStatus("ACCEPTED");
        t.setQueuePosition((int) processing + 1);
        t.setTotalCount(wish.size());
        t.setAcceptedCount(0);
        t.setRejectedCount(0);
        t.setSubmittedAt(LocalDateTime.now());
        t.setDeleted(0);
        ticketMapper.insert(t);

        // 返回给前端的中文状态
        Map<String, Object> ticketResp = new LinkedHashMap<>();
        ticketResp.put("id", t.getId());
        ticketResp.put("status", "已受理");
        ticketResp.put("queuePos", t.getQueuePosition());
        ticketResp.put("total", wish.size());
        ticketResp.put("accepted", new ArrayList<>());
        ticketResp.put("rejected", new ArrayList<>());

        // 异步处理：模拟 MQ 落库 + 结果推送
        CompletableFuture.delayedExecutor(900, TimeUnit.MILLISECONDS, executor)
                .execute(() -> processTicket(t.getId(), u.getStudentId()));

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("ticket", ticketResp);
        out.put("message", "已受理，正在处理");
        return out;
    }

    private void processTicket(Long ticketId, Long sid) {
        try {
            SelectionTicket t = ticketMapper.selectById(ticketId);
            if (t == null) return;
            t.setStatus("PROCESSING");
            ticketMapper.updateById(t);
            List<Wishlist> wish = wishlistMapper.selectList(new QueryWrapper<Wishlist>().eq("student_id", sid).orderByAsc("priority"));
            List<Course> enrolled = enrolledCourses(sid);
            SelectionRule rule = courseService.rule();
            List<Map<String, Object>> accepted = new ArrayList<>();
            List<Map<String, Object>> rejected = new ArrayList<>();
            List<Long> acceptedIds = new ArrayList<>();
            for (Wishlist w : wish) {
                Course c = courseMapper.selectById(w.getCourseId());
                if (c == null) continue;
                List<Map<String, Object>> cd = courseService.detectConflicts(c, enrolled, rule);
                if (cd.stream().anyMatch(x -> "hard".equals(x.get("level")))) {
                    String reason = cd.stream().filter(x -> "hard".equals(x.get("level"))).findFirst().get().get("message").toString();
                    rejected.add(mapOf("courseId", c.getId(), "name", c.getName(), "reason", reason));
                    addTicketItem(ticketId, c.getId(), c.getName(), "REJECTED", reason);
                    continue;
                }
                if (totalCreditsAfter(sid, acceptedIds, c).compareTo(rule.getCreditLimit()) > 0) {
                    String reason = "超出学分上限（" + rule.getCreditLimit() + " 学分）";
                    rejected.add(mapOf("courseId", c.getId(), "name", c.getName(), "reason", reason));
                    addTicketItem(ticketId, c.getId(), c.getName(), "REJECTED", reason);
                    continue;
                }
                if (c.getRemaining() <= 0) {
                    String reason = "名额已满";
                    rejected.add(mapOf("courseId", c.getId(), "name", c.getName(), "reason", reason));
                    addTicketItem(ticketId, c.getId(), c.getName(), "REJECTED", reason);
                    continue;
                }
                // 占用名额 + 落选课记录
                c.setEnrolled(c.getEnrolled() + 1);
                courseMapper.updateById(c);
                StudentCourse sc = new StudentCourse();
                sc.setStudentId(sid);
                sc.setCourseId(c.getId());
                sc.setStatus("SELECTED");
                sc.setSource("SELECTION");
                sc.setSelectedAt(LocalDateTime.now());
                studentCourseMapper.insert(sc);
                accepted.add(mapOf("courseId", c.getId(), "name", c.getName(), "reason", "选课成功"));
                acceptedIds.add(c.getId());
                addTicketItem(ticketId, c.getId(), c.getName(), "ACCEPTED", null);
            }
            // 移除已选成功的心愿单
            if (!acceptedIds.isEmpty()) {
                for (Long cid : acceptedIds) wishlistMapper.delete(new QueryWrapper<Wishlist>()
                        .eq("student_id", sid).eq("course_id", cid));
            }
            t.setAcceptedCount(accepted.size());
            t.setRejectedCount(rejected.size());
            t.setStatus(accepted.isEmpty() ? "FAILED" : "SUCCESS");
            t.setFinishedAt(LocalDateTime.now());
            ticketMapper.updateById(t);
            addResultMessage(sid, accepted, rejected);
        } catch (Exception e) {
            SelectionTicket t = ticketMapper.selectById(ticketId);
            if (t != null) {
                t.setStatus("FAILED");
                t.setFinishedAt(LocalDateTime.now());
                ticketMapper.updateById(t);
            }
        }
    }

    private BigDecimal totalCreditsAfter(Long sid, List<Long> acceptedIds, Course c) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Course e : enrolledCourses(sid)) if (e.getCredits() != null) sum = sum.add(e.getCredits());
        for (Long id : acceptedIds) {
            Course e = courseMapper.selectById(id);
            if (e != null && e.getCredits() != null) sum = sum.add(e.getCredits());
        }
        if (c.getCredits() != null) sum = sum.add(c.getCredits());
        return sum;
    }

    private void addTicketItem(Long ticketId, Long courseId, String name, String result, String reason) {
        SelectionTicketItem it = new SelectionTicketItem();
        it.setTicketId(ticketId);
        it.setCourseId(courseId);
        it.setCourseName(name);
        it.setResult(result);
        it.setReason(reason);
        ticketItemMapper.insert(it);
    }

    private void addResultMessage(Long sid, List<Map<String, Object>> accepted, List<Map<String, Object>> rejected) {
        Message m = new Message();
        m.setUserId(userIdOfStudent(sid));
        m.setType("result");
        m.setTitle("选课结果：成功 " + accepted.size() + " 门，失败 " + rejected.size() + " 门");
        m.setBody(accepted.stream().map(a -> (String) a.get("name")).collect(Collectors.joining("、")).isEmpty()
                ? "无成功项" : accepted.stream().map(a -> (String) a.get("name")).collect(Collectors.joining("、")));
        m.setIsRead(0);
        m.setCreatedAt(LocalDateTime.now());
        messageMapper.insert(m);
    }

    private Long userIdOfStudent(Long sid) {
        // 简化：学生档案 user_id 与 student.id 相同（种子数据如此），否则按 student 查询
        return sid;
    }

    public Map<String, Object> ticketStatus(Long id, LoginUser u) {
        SelectionTicket t = ticketMapper.selectById(id);
        if (t == null) throw new ApiException(404, "票据不存在", null);
        List<Map<String, Object>> accepted = new ArrayList<>();
        List<Map<String, Object>> rejected = new ArrayList<>();
        for (SelectionTicketItem it : ticketItemMapper.selectList(new QueryWrapper<SelectionTicketItem>().eq("ticket_id", id))) {
            if ("ACCEPTED".equals(it.getResult())) accepted.add(mapOf("courseId", it.getCourseId(), "name", it.getCourseName(), "reason", "选课成功"));
            else rejected.add(mapOf("courseId", it.getCourseId(), "name", it.getCourseName(), "reason", it.getReason()));
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId());
        m.put("status", zhStatus(t.getStatus()));
        m.put("queuePos", t.getQueuePosition());
        m.put("total", t.getTotalCount());
        m.put("accepted", accepted);
        m.put("rejected", rejected);
        m.put("createdAt", fmt(t.getSubmittedAt()));
        return m;
    }

    private String zhStatus(String s) {
        if (s == null) return "处理中";
        return switch (s) {
            case "ACCEPTED" -> "已受理";
            case "PROCESSING" -> "处理中";
            case "SUCCESS", "PARTIAL" -> "成功";
            case "FAILED" -> "失败";
            default -> "处理中";
        };
    }

    /* ================= 课表 ================= */

    public Map<String, Object> timetable(LoginUser u) {
        UserPreference pref = prefOf(u);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("view", pref == null ? "week" : pref.getTimetableView());
        out.put("totalCredits", totalCredits(u.getStudentId()));
        List<Map<String, Object>> courses = enrolledCourses(u.getStudentId()).stream().map(c -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("name", c.getName());
            m.put("teacher", c.getTeacherName());
            m.put("place", c.getPlace());
            m.put("campus", c.getCampus());
            m.put("credits", c.getCredits());
            m.put("schedule", courseService.schedules(c.getId()));
            return m;
        }).collect(Collectors.toList());
        out.put("courses", courses);
        return out;
    }

    /* ================= 消息 ================= */

    public Map<String, Object> messages(LoginUser u) {
        List<Map<String, Object>> items = messageMapper.selectList(new QueryWrapper<Message>()
                        .eq("user_id", u.getUserId()).orderByDesc("created_at"))
                .stream().map(m -> {
                    Map<String, Object> x = new LinkedHashMap<>();
                    x.put("id", m.getId());
                    x.put("type", m.getType());
                    x.put("title", m.getTitle());
                    x.put("body", m.getBody());
                    x.put("time", fmt(m.getCreatedAt()));
                    x.put("read", m.getIsRead() != null && m.getIsRead() == 1);
                    return x;
                }).collect(Collectors.toList());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        return out;
    }

    @Transactional
    public Map<String, Object> markRead(LoginUser u) {
        List<Message> list = messageMapper.selectList(new QueryWrapper<Message>().eq("user_id", u.getUserId()).eq("is_read", 0));
        for (Message m : list) { m.setIsRead(1); messageMapper.updateById(m); }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        return out;
    }

    /* ================= 偏好 ================= */

    public UserPreference prefOf(LoginUser u) {
        UserPreference p = prefMapper.selectOne(new QueryWrapper<UserPreference>().eq("user_id", u.getUserId()));
        if (p == null) {
            p = new UserPreference();
            p.setUserId(u.getUserId());
            p.setTheme("light");
            p.setDensity("standard");
            p.setFontScale("standard");
            p.setTimetableView("week");
            p.setCompactFilter(0);
            p.setNotifyResult(1); p.setNotifySeat(1); p.setNotifySystem(1); p.setNotifyDrop(1);
            p.setUpdatedAt(LocalDateTime.now());
            prefMapper.insert(p);
        }
        return p;
    }

    public Map<String, Object> preferences(LoginUser u) {
        UserPreference p = prefOf(u);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("theme", p.getTheme());
        m.put("density", p.getDensity());
        m.put("timetableView", p.getTimetableView());
        m.put("compactFilter", p.getCompactFilter() != null && p.getCompactFilter() == 1);
        m.put("fontScale", p.getFontScale());
        Map<String, Object> notify = new LinkedHashMap<>();
        notify.put("result", p.getNotifyResult() != null && p.getNotifyResult() == 1);
        notify.put("seat", p.getNotifySeat() != null && p.getNotifySeat() == 1);
        notify.put("system", p.getNotifySystem() != null && p.getNotifySystem() == 1);
        notify.put("drop", p.getNotifyDrop() != null && p.getNotifyDrop() == 1);
        m.put("notify", notify);
        return m;
    }

    @Transactional
    public Map<String, Object> setPreferences(Map<String, Object> patch, LoginUser u) {
        UserPreference p = prefOf(u);
        if (patch.get("theme") != null) p.setTheme(String.valueOf(patch.get("theme")));
        if (patch.get("density") != null) p.setDensity(String.valueOf(patch.get("density")));
        if (patch.get("fontScale") != null) p.setFontScale(String.valueOf(patch.get("fontScale")));
        if (patch.get("timetableView") != null) p.setTimetableView(String.valueOf(patch.get("timetableView")));
        if (patch.get("compactFilter") != null) p.setCompactFilter(Boolean.TRUE.equals(patch.get("compactFilter")) ? 1 : 0);
        if (patch.get("notify") instanceof Map) {
            Map<?, ?> n = (Map<?, ?>) patch.get("notify");
            if (n.get("result") != null) p.setNotifyResult(Boolean.TRUE.equals(n.get("result")) ? 1 : 0);
            if (n.get("seat") != null) p.setNotifySeat(Boolean.TRUE.equals(n.get("seat")) ? 1 : 0);
            if (n.get("system") != null) p.setNotifySystem(Boolean.TRUE.equals(n.get("system")) ? 1 : 0);
            if (n.get("drop") != null) p.setNotifyDrop(Boolean.TRUE.equals(n.get("drop")) ? 1 : 0);
        }
        p.setUpdatedAt(LocalDateTime.now());
        prefMapper.updateById(p);
        return preferences(u);
    }

    /* ================= 放号订阅 ================= */

    public Map<String, Object> subscriptions(LoginUser u) {
        List<Map<String, Object>> items = subscriptionMapper.selectList(new QueryWrapper<SeatSubscription>()
                        .eq("student_id", u.getStudentId()).eq("status", "ACTIVE"))
                .stream().map(s -> {
                    Course c = courseMapper.selectById(s.getCourseId());
                    return c == null ? null : courseService.enrich(c);
                }).filter(x -> x != null).collect(Collectors.toList());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        return out;
    }

    @Transactional
    public Map<String, Object> subscribe(Long courseId, LoginUser u) {
        Course c = courseMapper.selectById(courseId);
        if (c == null) throw new ApiException(400, "课程不存在", null);
        if (c.getRemaining() > 0) throw new ApiException(400, "该课程尚有名额，无需订阅", null);
        if (subscriptionMapper.selectCount(new QueryWrapper<SeatSubscription>()
                .eq("student_id", u.getStudentId()).eq("course_id", courseId).eq("status", "ACTIVE")) > 0)
            throw new ApiException(400, "已订阅该课程的放号通知", null);
        SeatSubscription s = new SeatSubscription();
        s.setStudentId(u.getStudentId());
        s.setCourseId(courseId);
        s.setStatus("ACTIVE");
        s.setCreatedAt(LocalDateTime.now());
        subscriptionMapper.insert(s);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        return out;
    }

    @Transactional
    public Map<String, Object> unsubscribe(Long courseId, LoginUser u) {
        subscriptionMapper.update(null, new com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper<SeatSubscription>()
                .eq("student_id", u.getStudentId()).eq("course_id", courseId).eq("status", "ACTIVE").set("status", "CANCELLED"));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        return out;
    }

    private Map<String, Object> mapOf(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) m.put(String.valueOf(kv[i]), kv[i + 1]);
        return m;
    }

    private String fmt(LocalDateTime t) { return t == null ? "" : FMT.format(t); }

    /* ================= 学业闭环（成绩 / 学分 / 选课记录 / 公告 / 历史课表） ================= */

    /** 学业看板：成绩单 + 历年成绩 + 学分总览 + 选课记录 + 学生档案 */
    public Map<String, Object> studentScoreBoard(LoginUser u) {
        Long sid = u.getStudentId();
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("scores", scoreService.studentScores(sid, null).get("items"));
        out.put("scoreHistory", scoreService.studentScoreHistory(sid).get("semesters"));
        out.put("credits", creditsService.studentCredits(sid));
        out.put("records", records(sid, null).get("items"));
        Student st = studentMapper.selectById(sid);
        if (st != null) {
            Map<String, Object> profile = new LinkedHashMap<>();
            profile.put("studentNo", st.getStudentNo());
            profile.put("name", "");
            profile.put("major", st.getMajor());
            profile.put("grade", st.getGrade());
            profile.put("className", st.getClassName());
            profile.put("college", st.getCollege());
            profile.put("eduSystem", st.getEduSystem());
            profile.put("enrollmentDate", st.getEnrollmentDate() == null ? null : st.getEnrollmentDate().toString());
            profile.put("graduationDate", st.getGraduationDate() == null ? null : st.getGraduationDate().toString());
            SysUser su = sysUserMapper.selectById(st.getUserId());
            if (su != null) profile.put("name", su.getRealName());
            out.put("student", profile);
        }
        return out;
    }

    public Map<String, Object> studentScores(LoginUser u, Long semesterId) {
        return scoreService.studentScores(u.getStudentId(), semesterId);
    }

    public Map<String, Object> studentScoreHistory(LoginUser u) {
        return scoreService.studentScoreHistory(u.getStudentId());
    }

    public Map<String, Object> studentCredits(LoginUser u) {
        return creditsService.studentCredits(u.getStudentId());
    }

    /** 学生可见公告（置顶优先） */
    public Map<String, Object> announcements() {
        return announcementService.listPublished();
    }

    /** 选课记录（含课程名/学分/状态，可按学期过滤） */
    public Map<String, Object> records(Long sid, Long semesterId) {
        QueryWrapper<StudentCourse> qw = new QueryWrapper<StudentCourse>()
                .eq("student_id", sid).ne("status", "DROPPED")
                .orderByAsc("semester_id").orderByAsc("id");
        if (semesterId != null) qw.eq("semester_id", semesterId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (StudentCourse sc : studentCourseMapper.selectList(qw)) {
            Course c = courseMapper.selectById(sc.getCourseId());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", sc.getId());
            m.put("courseId", sc.getCourseId());
            m.put("semesterId", sc.getSemesterId());
            m.put("status", sc.getStatus());
            m.put("statusText", scStatus(sc.getStatus()));
            m.put("selectType", sc.getSelectType());
            m.put("selectedAt", sc.getSelectedAt() == null ? "" : sc.getSelectedAt().toString().replace("T", " "));
            if (c != null) {
                m.put("code", c.getCode());
                m.put("courseName", c.getName());
                m.put("category", c.getCategory());
                m.put("credits", c.getCredits());
                m.put("teacherName", c.getTeacherName());
            }
            items.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("total", items.size());
        return out;
    }

    private String scStatus(String s) {
        if (s == null) return "已选";
        return switch (s) {
            case "SELECTED" -> "已选";
            case "STUDYING" -> "在读";
            case "DROPPED" -> "已退";
            case "PENDING_SCORE" -> "待录成绩";
            case "ARCHIVED" -> "已归档";
            default -> s;
        };
    }

    /** 历史课表（按学期分组，含排课与课程详情） */
    public Map<String, Object> timetables(LoginUser u, Long semesterId) {
        Long sid = u.getStudentId();
        List<StudentCourse> scs = studentCourseMapper.selectList(new QueryWrapper<StudentCourse>()
                .eq("student_id", sid).ne("status", "DROPPED").orderByAsc("semester_id"));
        if (semesterId != null) {
            scs = scs.stream().filter(x -> semesterId.equals(x.getSemesterId())).collect(Collectors.toList());
        }
        Map<Long, List<Map<String, Object>>> bySem = new LinkedHashMap<>();
        for (StudentCourse sc : scs) {
            Course c = courseMapper.selectById(sc.getCourseId());
            if (c == null) continue;
            Long semId = sc.getSemesterId() == null ? -1L : sc.getSemesterId();
            List<Map<String, Object>> list = bySem.computeIfAbsent(semId, k -> new ArrayList<>());
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("code", c.getCode());
            m.put("name", c.getName());
            m.put("teacher", c.getTeacherName());
            m.put("campus", c.getCampus());
            m.put("place", c.getPlace());
            m.put("credits", c.getCredits());
            m.put("weeks", c.getWeeks());
            m.put("status", sc.getStatus());
            List<Map<String, Object>> sch = new ArrayList<>();
            for (CourseSchedule cs : scheduleMapper.selectList(new QueryWrapper<CourseSchedule>().eq("course_id", c.getId()))) {
                Map<String, Object> sm = new LinkedHashMap<>();
                sm.put("day", cs.getDayOfWeek());
                sm.put("start", cs.getStartPeriod());
                sm.put("end", cs.getEndPeriod());
                sm.put("place", cs.getPlace());
                sch.add(sm);
            }
            m.put("schedule", sch);
            list.add(m);
        }
        List<Map<String, Object>> semesters = new ArrayList<>();
        for (Map.Entry<Long, List<Map<String, Object>>> e : bySem.entrySet()) {
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("semesterId", e.getKey() < 0 ? null : e.getKey());
            Semester sem = e.getKey() < 0 ? null : semesterMapper.selectById(e.getKey());
            sm.put("semesterName", sem == null ? "历史学期" : sem.getName());
            sm.put("courses", e.getValue());
            semesters.add(sm);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("semesters", semesters);
        return out;
    }
}
