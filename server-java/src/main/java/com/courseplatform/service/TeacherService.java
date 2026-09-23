package com.courseplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.courseplatform.entity.Course;
import com.courseplatform.entity.CourseSchedule;
import com.courseplatform.entity.Score;
import com.courseplatform.entity.Student;
import com.courseplatform.entity.StudentCourse;
import com.courseplatform.entity.SysUser;
import com.courseplatform.exception.ApiException;
import com.courseplatform.mapper.CourseMapper;
import com.courseplatform.mapper.CourseScheduleMapper;
import com.courseplatform.mapper.ScoreMapper;
import com.courseplatform.mapper.StudentCourseMapper;
import com.courseplatform.mapper.StudentMapper;
import com.courseplatform.mapper.SysUserMapper;
import com.courseplatform.security.LoginUser;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 教师端：档案 / 我授课课程 / 选课名单 / 授课概览 / 成绩录入 / 统计 / 课表 */
@Service
public class TeacherService {

    private static final String SURNAMES = "王李张刘陈杨赵黄周吴徐孙马朱胡郭何高林罗郑梁谢宋唐许韩冯邓曹彭";
    private static final String[] GIVEN = {"伟", "芳", "娜", "敏", "静", "磊", "军", "洋", "勇", "艳", "杰", "涛", "明", "超",
            "雪", "晨", "宇", "浩", "子轩", "雨桐", "一诺", "欣怡", "浩然", "梓涵", "思远", "天翊", "若曦", "嘉懿", "沐宸", "奕辰"};

    private final CourseMapper courseMapper;
    private final CourseScheduleMapper scheduleMapper;
    private final ScoreService scoreService;
    private final SemesterService semesterService;
    private final ScoreMapper scoreMapper;
    private final StudentCourseMapper studentCourseMapper;
    private final StudentMapper studentMapper;
    private final SysUserMapper sysUserMapper;

    public TeacherService(CourseMapper courseMapper, CourseScheduleMapper scheduleMapper, ScoreService scoreService,
                          SemesterService semesterService, ScoreMapper scoreMapper, StudentCourseMapper studentCourseMapper,
                          StudentMapper studentMapper, SysUserMapper sysUserMapper) {
        this.courseMapper = courseMapper;
        this.scheduleMapper = scheduleMapper;
        this.scoreService = scoreService;
        this.semesterService = semesterService;
        this.scoreMapper = scoreMapper;
        this.studentCourseMapper = studentCourseMapper;
        this.studentMapper = studentMapper;
        this.sysUserMapper = sysUserMapper;
    }

    private List<Course> myCourses(LoginUser u) {
        List<Course> byId = courseMapper.selectList(new QueryWrapper<Course>().eq("teacher_id", u.getTeacherId()));
        if (byId != null && !byId.isEmpty()) return byId;
        return courseMapper.selectList(new QueryWrapper<Course>().eq("teacher_name", u.getRealName()));
    }

    public Map<String, Object> profile(LoginUser u) {
        List<Course> courses = myCourses(u);
        int totalStudents = courses.stream().mapToInt(c -> c.getEnrolled() == null ? 0 : c.getEnrolled()).sum();
        int totalCapacity = courses.stream().mapToInt(c -> c.getCapacity() == null ? 0 : c.getCapacity()).sum();
        int remaining = courses.stream().mapToInt(Course::getRemaining).sum();
        int fillRate = totalCapacity == 0 ? 0 : Math.round((totalStudents * 100.0f) / totalCapacity);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", u.getTeacherId());
        m.put("name", u.getRealName());
        m.put("title", u.getTitle());
        m.put("dept", u.getDept());
        m.put("courseCount", courses.size());
        m.put("totalStudents", totalStudents);
        m.put("totalCapacity", totalCapacity);
        m.put("remaining", remaining);
        m.put("fillRate", fillRate);
        return m;
    }

    public Map<String, Object> courses(LoginUser u) {
        List<Map<String, Object>> items = myCourses(u).stream().map(c -> {
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
            m.put("intro", c.getIntro());
            m.put("semesterId", c.getSemesterId());
            m.put("courseStatus", c.getCourseStatus());
            m.put("schedule", schedules(c.getId()));
            int cap = c.getCapacity() == null ? 0 : c.getCapacity();
            int enr = c.getEnrolled() == null ? 0 : c.getEnrolled();
            m.put("fillRate", cap == 0 ? 0 : Math.round(enr * 100.0f / cap));
            m.put("rosterCount", Math.min(enr, 30));
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        return out;
    }

    public List<Map<String, Object>> schedules(Long courseId) {
        return scheduleMapper.selectList(new QueryWrapper<CourseSchedule>().eq("course_id", courseId))
                .stream().map(s -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("day", s.getDayOfWeek());
                    m.put("start", s.getStartPeriod());
                    m.put("end", s.getEndPeriod());
                    m.put("place", s.getPlace());
                    return m;
                }).collect(Collectors.toList());
    }

    public Map<String, Object> roster(Long courseId, LoginUser u) {
        Course c = courseMapper.selectById(courseId);
        if (c == null) throw new ApiException(404, "课程不存在", null);
        if (!c.getTeacherName().equals(u.getRealName()) && !Long.valueOf(u.getTeacherId()).equals(c.getTeacherId()))
            throw new ApiException(403, "只能查看本人授课课程的名单", null);
        Map<String, Object> courseInfo = new LinkedHashMap<>();
        courseInfo.put("id", c.getId());
        courseInfo.put("name", c.getName());
        courseInfo.put("code", c.getCode());
        courseInfo.put("place", c.getPlace());
        courseInfo.put("schedule", schedules(courseId));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("course", courseInfo);
        out.put("students", genRoster(c));
        return out;
    }

    /** 按课程确定性生成选课名单（同一课程多次结果一致，对齐 Node genRoster） */
    private List<Map<String, Object>> genRoster(Course course) {
        java.util.Random rnd = new java.util.Random(course.getId() * 7919L + 13);
        int count = Math.min(course.getEnrolled() == null ? 0 : course.getEnrolled(), 30);
        Set<String> used = new HashSet<>();
        List<Map<String, Object>> list = new ArrayList<>();
        String major = (course.getCategory() != null && (course.getCategory().equals("体育") || course.getCategory().equals("通识")))
                ? "全校公选" : "计算机科学与技术";
        String[] grades = {"2023 级", "2024 级", "2025 级"};
        for (int i = 0; i < count; i++) {
            String name;
            do {
                name = String.valueOf(SURNAMES.charAt(rnd.nextInt(SURNAMES.length())))
                        + GIVEN[rnd.nextInt(GIVEN.length)];
            } while (used.contains(name));
            used.add(name);
            Map<String, Object> s = new LinkedHashMap<>();
            s.put("studentNo", "202" + (3 + rnd.nextInt(3)) + String.format("%05d", 10000 + rnd.nextInt(8999)));
            s.put("name", name);
            s.put("major", major);
            s.put("grade", grades[rnd.nextInt(grades.length)]);
            s.put("status", "已选");
            list.add(s);
        }
        list.sort(Comparator.comparing(a -> String.valueOf(a.get("status"))));
        return list;
    }

    public Map<String, Object> updateCourse(Long courseId, Map<String, Object> patch, LoginUser u) {
        Course c = courseMapper.selectById(courseId);
        if (c == null) throw new ApiException(404, "课程不存在", null);
        if (!c.getTeacherName().equals(u.getRealName()) && !Long.valueOf(u.getTeacherId()).equals(c.getTeacherId()))
            throw new ApiException(403, "只能编辑本人授课的课程", null);
        if (patch.get("intro") != null) { String v = String.valueOf(patch.get("intro")).trim(); if (!v.isEmpty()) c.setIntro(v.substring(0, Math.min(v.length(), 500))); }
        if (patch.get("place") != null) { String v = String.valueOf(patch.get("place")).trim(); if (!v.isEmpty()) c.setPlace(v.substring(0, Math.min(v.length(), 60))); }
        courseMapper.updateById(c);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("course", c);
        return out;
    }

    /* ================= 学业闭环 ================= */

    /** 授课概览：本学期课程数、学时统计、待录成绩课程数 */
    public Map<String, Object> overview(LoginUser u) {
        List<Course> courses = myCourses(u);
        var cur = semesterService.getCurrent();
        Long curSem = cur == null ? null : cur.getId();
        List<Course> termCourses = courses.stream()
                .filter(c -> c.getSemesterId() == null || c.getSemesterId().equals(curSem))
                .collect(Collectors.toList());
        int totalHours = 0;
        int termHours = 0;
        int pending = 0;
        for (Course c : courses) {
            int h = c.getTotalHours() != null ? c.getTotalHours()
                    : (c.getTheoryHours() == null ? 0 : c.getTheoryHours())
                    + (c.getPracticeHours() == null ? 0 : c.getPracticeHours())
                    + (c.getLabHours() == null ? 0 : c.getLabHours());
            totalHours += h;
            if (c.getSemesterId() == null || c.getSemesterId().equals(curSem)) termHours += h;
            if (hasPendingScores(c.getId())) pending++;
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("courseCount", courses.size());
        m.put("termCourseCount", termCourses.size());
        m.put("totalHours", totalHours);
        m.put("termHours", termHours);
        m.put("pendingScoreCourses", pending);
        m.put("semesterId", curSem);
        m.put("semesterName", cur == null ? "" : cur.getName());
        return m;
    }

    private boolean hasPendingScores(Long courseId) {
        long enroll = studentCourseMapper.selectCount(new QueryWrapper<StudentCourse>()
                .eq("course_id", courseId).ne("status", "DROPPED"));
        if (enroll == 0) return false;
        long total = scoreMapper.selectCount(new QueryWrapper<Score>().eq("course_id", courseId));
        if (total == 0) return true;
        return scoreMapper.selectCount(new QueryWrapper<Score>()
                .eq("course_id", courseId).eq("status", "待录入")) > 0;
    }

    public Map<String, Object> courseScores(LoginUser u, Long courseId) {
        return scoreService.teacherCourseScores(u.getTeacherId(), courseId);
    }

    public Map<String, Object> saveScores(LoginUser u, Long courseId, List<Map<String, Object>> items) {
        return scoreService.teacherSaveScores(u.getTeacherId(), courseId, items);
    }

    public Map<String, Object> courseStats(Long courseId) {
        return scoreService.courseStats(courseId);
    }

    public Map<String, Object> failList(Long courseId) {
        return scoreService.failList(courseId);
    }

    /** 教师授课课表：JOIN course_schedule + course */
    public Map<String, Object> teacherTimetable(LoginUser u) {
        List<Course> courses = myCourses(u);
        List<Map<String, Object>> items = new ArrayList<>();
        for (Course c : courses) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", c.getId());
            m.put("code", c.getCode());
            m.put("name", c.getName());
            m.put("category", c.getCategory());
            m.put("credits", c.getCredits());
            m.put("campus", c.getCampus());
            m.put("place", c.getPlace());
            m.put("assessment", c.getAssessment());
            m.put("semesterId", c.getSemesterId());
            m.put("courseStatus", c.getCourseStatus());
            m.put("enrolled", c.getEnrolled());
            m.put("capacity", c.getCapacity());
            m.put("schedule", schedules(c.getId()));
            items.add(m);
        }
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("total", items.size());
        return out;
    }
}
