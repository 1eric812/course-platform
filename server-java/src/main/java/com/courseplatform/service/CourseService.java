package com.courseplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.courseplatform.entity.Course;
import com.courseplatform.entity.CourseSchedule;
import com.courseplatform.entity.SelectionRule;
import com.courseplatform.entity.StudentCourse;
import com.courseplatform.entity.Wishlist;
import com.courseplatform.entity.SeatSubscription;
import com.courseplatform.exception.ApiException;
import com.courseplatform.mapper.CourseMapper;
import com.courseplatform.mapper.CourseScheduleMapper;
import com.courseplatform.mapper.SelectionRuleMapper;
import com.courseplatform.mapper.StudentCourseMapper;
import com.courseplatform.mapper.WishlistMapper;
import com.courseplatform.mapper.SeatSubscriptionMapper;
import com.courseplatform.security.LoginUser;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/** 课程中心：筛选、冲突检测、课程列表与详情 */
@Service
public class CourseService {

    private final CourseMapper courseMapper;
    private final CourseScheduleMapper scheduleMapper;
    private final StudentCourseMapper studentCourseMapper;
    private final WishlistMapper wishlistMapper;
    private final SeatSubscriptionMapper subscriptionMapper;
    private final SelectionRuleMapper ruleMapper;

    public CourseService(CourseMapper courseMapper, CourseScheduleMapper scheduleMapper,
                         StudentCourseMapper studentCourseMapper, WishlistMapper wishlistMapper,
                         SeatSubscriptionMapper subscriptionMapper, SelectionRuleMapper ruleMapper) {
        this.courseMapper = courseMapper;
        this.scheduleMapper = scheduleMapper;
        this.studentCourseMapper = studentCourseMapper;
        this.wishlistMapper = wishlistMapper;
        this.subscriptionMapper = subscriptionMapper;
        this.ruleMapper = ruleMapper;
    }

    private static final String[] DAY_NAMES = {"一", "二", "三", "四", "五", "六", "日"};
    private static final List<String> CATEGORY_ORDER = List.of("必修", "选修", "通识", "体育");

    public SelectionRule rule() {
        return ruleMapper.selectById(1L);
    }

    /* ---------- 数据装配 ---------- */

    public List<Map<String, Object>> schedules(Long courseId) {
        return scheduleMapper.selectList(new QueryWrapper<CourseSchedule>().eq("course_id", courseId))
                .stream().map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("day", s.getDayOfWeek());
                    m.put("start", s.getStartPeriod());
                    m.put("end", s.getEndPeriod());
                    return m;
                }).collect(Collectors.toList());
    }

    private List<String> tags(Course c) {
        if (c.getRemaining() <= 0) return List.of("已满");
        if (c.getRemaining() <= 8) return List.of("名额紧张");
        return new ArrayList<>();
    }

    public Map<String, Object> enrich(Course c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", c.getId());
        m.put("code", c.getCode());
        m.put("name", c.getName());
        m.put("category", c.getCategory());
        m.put("credits", c.getCredits());
        m.put("teacher", c.getTeacherName());
        m.put("campus", c.getCampus());
        m.put("place", c.getPlace());
        m.put("assessment", c.getAssessment());
        m.put("capacity", c.getCapacity());
        m.put("enrolled", c.getEnrolled());
        m.put("remaining", c.getRemaining());
        m.put("rating", c.getRating());
        m.put("prereq", c.getPrereqName());
        m.put("tags", tags(c));
        m.put("intro", c.getIntro());
        m.put("schedule", schedules(c.getId()));
        return m;
    }

    /** 兼容旧 Node 版服务层：返回所有课程摘要列表。 */
    public List<Map<String, Object>> catalog() {
        return courseMapper.selectList(null).stream().map(this::enrich).collect(Collectors.toList());
    }

    /** 兼容旧 Node 版服务层：按 courseId 聚合课表。 */
    public Map<Long, List<Map<String, Object>>> scheduleMap() {
        Map<Long, List<Map<String, Object>>> out = new LinkedHashMap<>();
        for (CourseSchedule s : scheduleMapper.selectList(null)) {
            if (s == null || s.getCourseId() == null) continue;
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("day", s.getDayOfWeek());
            item.put("start", s.getStartPeriod());
            item.put("end", s.getEndPeriod());
            out.computeIfAbsent(s.getCourseId(), k -> new ArrayList<>()).add(item);
        }
        return out;
    }

    /** 兼容旧 Node 版服务层：补齐 schedule 字段。 */
    public void decorate(Map<String, Object> course, List<Map<String, Object>> schedule) {
        if (course == null) return;
        course.put("schedule", schedule == null ? new ArrayList<>() : schedule);
    }

    /** 兼容旧 Node 版服务层：学生已选课程列表（Map 视图）。 */
    public List<Map<String, Object>> enrolledCourses(long studentId) {
        return enrolledCourses(Long.valueOf(studentId)).stream().map(this::enrich).collect(Collectors.toList());
    }

    /** 兼容旧 Node 版服务层：累计学分。 */
    public double sumCredits(List<Map<String, Object>> courses) {
        double total = 0d;
        for (Map<String, Object> c : courses) {
            Object raw = c.get("credits");
            if (raw == null) continue;
            if (raw instanceof Number n) {
                total += n.doubleValue();
            } else if (raw instanceof String s && !s.isBlank()) {
                total += Double.parseDouble(s);
            }
        }
        return total;
    }

    /** 兼容旧 Node 版服务层：学生学分上限。 */
    public double creditLimit(long studentId) {
        SelectionRule rule = rule();
        if (rule == null || rule.getCreditLimit() == null) {
            return 30d;
        }
        return rule.getCreditLimit().doubleValue();
    }

    /** 兼容旧 Node 版服务层：只保留硬冲突。 */
    public List<Map<String, Object>> hardOnly(List<Map<String, Object>> conflicts) {
        if (conflicts == null) return new ArrayList<>();
        return conflicts.stream().filter(c -> "hard".equals(c.get("level"))).collect(Collectors.toList());
    }

    /** 兼容旧 Node 版服务层：按 Map 结构执行冲突检测。 */
    public List<Map<String, Object>> detectConflicts(Map<String, Object> course, List<Map<String, Object>> enrolled,
                                                    Map<String, Object> rules, double used) {
        if (course == null) return new ArrayList<>();
        SelectionRule rule = new SelectionRule();
        if (rules != null) {
            Object block = rules.get("blockOnConflict");
            Object allowCross = rules.get("allowCrossCampus");
            if (block != null) rule.setBlockOnConflict(Integer.valueOf(String.valueOf(block)));
            if (allowCross != null) rule.setAllowCrossCampus(Integer.valueOf(String.valueOf(allowCross)));
            if (rules.get("creditLimit") != null) {
                rule.setCreditLimit(new BigDecimal(String.valueOf(rules.get("creditLimit"))));
            }
        }
        return detectConflicts(toCourse(course), enrolled == null ? new ArrayList<>() : enrolled.stream().map(this::toCourse).collect(Collectors.toList()), rule);
    }

    private Course toCourse(Map<String, Object> data) {
        if (data == null) return null;
        Course c = new Course();
        c.setId(data.get("id") instanceof Number n ? n.longValue() : null);
        c.setCode(String.valueOf(data.getOrDefault("code", "")));
        c.setName(String.valueOf(data.getOrDefault("name", "")));
        c.setCategory(String.valueOf(data.getOrDefault("category", "")));
        Object credits = data.get("credits");
        c.setCredits(credits instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : new BigDecimal(String.valueOf(credits == null ? "0" : credits)));
        c.setTeacherName(String.valueOf(data.getOrDefault("teacher", "")));
        c.setCampus(String.valueOf(data.getOrDefault("campus", "")));
        c.setPlace(String.valueOf(data.getOrDefault("place", "")));
        c.setAssessment(String.valueOf(data.getOrDefault("assessment", "")));
        c.setCapacity(data.get("capacity") instanceof Number n ? n.intValue() : 0);
        c.setEnrolled(data.get("enrolled") instanceof Number n ? n.intValue() : 0);
        c.setRemaining(data.get("remaining") instanceof Number n ? n.intValue() : 0);
        Object rating = data.get("rating");
        c.setRating(rating instanceof Number n ? BigDecimal.valueOf(n.doubleValue()) : new BigDecimal(String.valueOf(rating == null ? "0" : rating)));
        c.setPrereqName(String.valueOf(data.getOrDefault("prereq", "")));
        c.setIntro(String.valueOf(data.getOrDefault("intro", "")));
        return c;
    }

    /* ---------- 冲突检测（对齐 Node service.js） ---------- */

    private boolean overlap(Map<String, Object> a, Map<String, Object> b) {
        return a.get("day").equals(b.get("day"))
                && (int) a.get("start") <= (int) b.get("end")
                && (int) b.get("start") <= (int) a.get("end");
    }

    private List<Map<String, Object>> timeConflicts(Map<String, Object> a, Map<String, Object> b) {
        List<Map<String, Object>> hits = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sa = (List<Map<String, Object>>) a.get("schedule");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sb = (List<Map<String, Object>>) b.get("schedule");
        for (Map<String, Object> x : sa) for (Map<String, Object> y : sb)
            if (overlap(x, y)) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("day", x.get("day"));
                m.put("period", Math.max((int) x.get("start"), (int) y.get("start")) + "-" + Math.min((int) x.get("end"), (int) y.get("end")));
                hits.add(m);
            }
        return hits;
    }

    private Map<String, Object> commuteConflict(Map<String, Object> a, Map<String, Object> b) {
        String ca = (String) a.get("campus"), cb = (String) b.get("campus");
        if (ca == null || cb == null || ca.equals(cb)) return null;
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sa = (List<Map<String, Object>>) a.get("schedule");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> sb = (List<Map<String, Object>>) b.get("schedule");
        for (Map<String, Object> x : sa) for (Map<String, Object> y : sb) {
            if (!x.get("day").equals(y.get("day"))) continue;
            if (Math.abs((int) x.get("end") - (int) y.get("start")) <= 1 || Math.abs((int) y.get("end") - (int) x.get("start")) <= 1) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("day", x.get("day"));
                m.put("from", ca);
                m.put("to", cb);
                return m;
            }
        }
        return null;
    }

    public List<Map<String, Object>> detectConflicts(Course course, List<Course> others, SelectionRule rule) {
        Map<String, Object> cur = enrich(course);
        List<Map<String, Object>> result = new ArrayList<>();
        boolean block = rule.getBlockOnConflict() != null && rule.getBlockOnConflict() == 1;
        boolean allowCross = rule.getAllowCrossCampus() != null && rule.getAllowCrossCampus() == 1;
        for (Course o : others) {
            if (o.getId().equals(course.getId())) continue;
            Map<String, Object> om = enrich(o);
            List<Map<String, Object>> tc = timeConflicts(cur, om);
            if (!tc.isEmpty()) {
                Map<String, Object> x = tc.get(0);
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("type", "time");
                m.put("level", block ? "hard" : "soft");
                m.put("courseId", o.getId());
                m.put("courseName", o.getName());
                m.put("message", "与「" + o.getName() + "」时间冲突（周" + DAY_NAMES[(int) x.get("day") - 1] + " 第 " + x.get("period") + " 节）");
                result.add(m);
            }
            Map<String, Object> cc = commuteConflict(cur, om);
            if (cc != null && result.stream().noneMatch(r -> r.get("courseId").equals(o.getId()) && "commute".equals(r.get("type")))) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("type", "commute");
                m.put("level", allowCross ? "soft" : "hard");
                m.put("courseId", o.getId());
                m.put("courseName", o.getName());
                m.put("message", "与「" + o.getName() + "」跨校区连堂（" + cc.get("from") + " → " + cc.get("to") + "），通勤时间紧张");
                result.add(m);
            }
        }
        // 先修缺失（软）
        if (course.getPrereqName() != null && !course.getPrereqName().isBlank()) {
            boolean has = others.stream().anyMatch(o -> course.getPrereqName().equals(o.getName()));
            if (!has) {
                Map<String, Object> m = new LinkedHashMap<>();
                m.put("type", "prereq"); m.put("level", "soft");
                m.put("courseId", course.getId()); m.put("courseName", course.getName());
                m.put("message", "先修要求「" + course.getPrereqName() + "」未修读，建议先完成先修课");
                result.add(m);
            }
        }
        return result;
    }

    /* ---------- 接口 ---------- */

    public Map<String, Object> filterOptions() {
        List<Course> cs = courseMapper.selectList(null);
        Map<String, Object> out = new LinkedHashMap<>();
        Set<String> campus = new TreeSet<>();
        Set<String> assessment = new TreeSet<>();
        Set<String> teachers = new TreeSet<>();
        Set<Integer> credits = new TreeSet<>();
        Set<Integer> days = new TreeSet<>();
        Set<Integer> periods = new TreeSet<>();
        for (Course c : cs) {
            if (c.getCampus() != null) campus.add(c.getCampus());
            if (c.getAssessment() != null) assessment.add(c.getAssessment());
            if (c.getTeacherName() != null) teachers.add(c.getTeacherName());
            if (c.getCredits() != null) credits.add(c.getCredits().intValue());
            for (Map<String, Object> s : schedules(c.getId())) {
                days.add((int) s.get("day"));
                periods.add((int) s.get("start"));
            }
        }
        out.put("category", CATEGORY_ORDER.stream().filter(x -> cs.stream().anyMatch(c -> x.equals(c.getCategory()))).collect(Collectors.toList()));
        out.put("campus", new ArrayList<>(campus));
        out.put("assessment", new ArrayList<>(assessment));
        out.put("credits", credits.stream().sorted().collect(Collectors.toList()));
        out.put("days", days.stream().sorted().collect(Collectors.toList()));
        out.put("periods", periods.stream().sorted().collect(Collectors.toList()));
        out.put("teachers", new ArrayList<>(teachers));
        return out;
    }

    private List<Course> enrolledCourses(Long studentId) {
        if (studentId == null) return new ArrayList<>();
        List<Long> ids = studentCourseMapper.selectList(new QueryWrapper<StudentCourse>()
                .eq("student_id", studentId).eq("status", "SELECTED")).stream()
                .map(StudentCourse::getCourseId).collect(Collectors.toList());
        if (ids.isEmpty()) return new ArrayList<>();
        return courseMapper.selectBatchIds(ids);
    }

    public Map<String, Object> listCourses(Map<String, String> q, LoginUser u) {
        List<Course> all = courseMapper.selectList(null);
        boolean isStudent = "STUDENT".equals(u.getRole());
        Long sid = u.getStudentId();
        List<Course> enrolled = isStudent ? enrolledCourses(sid) : new ArrayList<>();
        Set<Long> enrolledIds = enrolled.stream().map(Course::getId).collect(Collectors.toSet());
        Set<Long> wishIds = new HashSet<>();
        Set<Long> subIds = new HashSet<>();
        if (isStudent && sid != null) {
            wishIds = wishlistMapper.selectList(new QueryWrapper<Wishlist>().eq("student_id", sid)).stream()
                    .map(Wishlist::getCourseId).collect(Collectors.toSet());
            subIds = subscriptionMapper.selectList(new QueryWrapper<SeatSubscription>()
                    .eq("student_id", sid).eq("status", "ACTIVE")).stream()
                    .map(SeatSubscription::getCourseId).collect(Collectors.toSet());
        }
        SelectionRule rule = rule();

        List<Map<String, Object>> items = new ArrayList<>();
        for (Course c : all) {
            Map<String, Object> m = enrich(c);
            boolean selected = enrolledIds.contains(c.getId());
            m.put("selected", selected);
            m.put("inWishlist", wishIds.contains(c.getId()));
            m.put("subscribed", subIds.contains(c.getId()));
            if (selected) {
                m.put("conflict", false);
                m.put("conflictDetail", new ArrayList<>());
            } else {
                List<Map<String, Object>> cd = detectConflicts(c, enrolled, rule);
                m.put("conflict", cd.stream().anyMatch(x -> "hard".equals(x.get("level"))));
                m.put("conflictDetail", cd);
            }
            items.add(m);
        }

        String kw = str(q.get("keyword"));
        if (!kw.isEmpty()) {
            String k = kw.toLowerCase();
            items.removeIf(m -> !String.valueOf(m.get("name")).toLowerCase().contains(k)
                    && !String.valueOf(m.get("code")).toLowerCase().contains(k)
                    && !String.valueOf(m.get("teacher")).toLowerCase().contains(k));
        }
        items = filterSet(items, q.get("category"), "category");
        items = filterSet(items, q.get("campus"), "campus");
        items = filterSet(items, q.get("assessment"), "assessment");
        if (q.get("credits") != null && !q.get("credits").isBlank()) {
            Set<Integer> sel = splitInts(q.get("credits"));
            items.removeIf(m -> !sel.contains(((BigDecimal) m.get("credits")).intValue()));
        }
        if (q.get("days") != null && !q.get("days").isBlank()) {
            Set<Integer> sel = splitInts(q.get("days"));
            items.removeIf(m -> ((List<?>) m.get("schedule")).stream().noneMatch(s ->
                    sel.contains((int) ((Map<?, ?>) s).get("day"))));
        }
        if (q.get("periods") != null && !q.get("periods").isBlank()) {
            Set<Integer> sel = splitInts(q.get("periods"));
            items.removeIf(m -> ((List<?>) m.get("schedule")).stream().noneMatch(s ->
                    sel.contains((int) ((Map<?, ?>) s).get("start"))));
        }
        String t = str(q.get("teacher"));
        if (!t.isEmpty()) items.removeIf(m -> !String.valueOf(m.get("teacher")).toLowerCase().contains(t.toLowerCase()));
        if ("1".equals(q.get("onlyNoConflict"))) items.removeIf(m -> Boolean.TRUE.equals(m.get("conflict")));
        if ("1".equals(q.get("onlyAvailable"))) items.removeIf(m -> (int) m.get("remaining") <= 0);
        if ("1".equals(q.get("excludeEnrolled"))) items.removeIf(m -> Boolean.TRUE.equals(m.get("selected")));

        String sort = q.get("sort") == null ? "default" : q.get("sort");
        Comparator<Map<String, Object>> comp;
        if ("remaining".equals(sort)) comp = Comparator.comparingInt((Map<String, Object> m) -> (int) m.get("remaining")).reversed();
        else if ("rating".equals(sort)) comp = Comparator.comparing((Map<String, Object> m) -> (BigDecimal) m.get("rating")).reversed();
        else if ("credits".equals(sort)) comp = Comparator.comparing((Map<String, Object> m) -> (BigDecimal) m.get("credits")).reversed();
        else comp = Comparator.comparingLong(m -> (long) m.get("id"));
        items.sort(comp);

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("total", items.size());
        out.put("credits", totalCredits(enrolledIds, all));
        out.put("creditLimit", rule.getCreditLimit());
        out.put("items", items);
        return out;
    }

    private BigDecimal totalCredits(Set<Long> enrolledIds, List<Course> all) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Course c : all) if (enrolledIds.contains(c.getId()) && c.getCredits() != null) sum = sum.add(c.getCredits());
        return sum;
    }

    private List<Map<String, Object>> filterSet(List<Map<String, Object>> items, String param, String key) {
        if (param == null || param.isBlank()) return items;
        Set<String> sel = new HashSet<>(List.of(param.split(",")));
        items.removeIf(m -> !sel.contains(String.valueOf(m.get(key))));
        return items;
    }

    private Set<Integer> splitInts(String s) {
        return java.util.Arrays.stream(s.split(",")).map(String::trim).filter(x -> !x.isEmpty())
                .map(Integer::parseInt).collect(Collectors.toSet());
    }

    private String str(String s) { return s == null ? "" : s.trim(); }

    public Map<String, Object> courseDetail(Long id, LoginUser u) {
        Course c = courseMapper.selectById(id);
        if (c == null) throw new ApiException(404, "课程不存在", null);
        Map<String, Object> m = enrich(c);
        Long sid = u.getStudentId();
        boolean isStudent = "STUDENT".equals(u.getRole());
        boolean selected = isStudent && sid != null && studentCourseMapper.selectCount(new QueryWrapper<StudentCourse>()
                .eq("student_id", sid).eq("course_id", id).eq("status", "SELECTED")) > 0;
        List<Map<String, Object>> conflicts = selected ? new ArrayList<>() : detectConflicts(c, enrolledCourses(sid), rule());
        m.put("conflicts", conflicts);
        m.put("selected", selected);
        return m;
    }
}
