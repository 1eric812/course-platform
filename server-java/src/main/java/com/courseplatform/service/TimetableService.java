package com.courseplatform.service;

import com.courseplatform.mapper.CoreMapper;
import com.courseplatform.support.Values;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 课表服务（对应原 server/service.js 的 timetable / exportIcs）。
 * 周视图与列表视图共用同一份 courses 数据（每门课含 schedule[{day,start,end}]）；
 * .ics 导出按「学期首周一 + 节次时间表」折算真实起止时间，导入系统日历即可使用。
 */
@Service
public class TimetableService {

    private static final String SEMESTER = "2026-2027 学年第一学期";
    /** 学期首周周一（用于把「周几 + 节次」折算为真实日期）。 */
    private static final LocalDate SEMESTER_MONDAY = LocalDate.of(2026, 9, 7);
    private static final LocalTime FIRST_PERIOD = LocalTime.of(8, 0);
    private static final int PERIOD_MINUTES = 45;
    private static final int BREAK_MINUTES = 10;
    private static final int WEEKS = 16;

    private static final DateTimeFormatter ICS_STAMP = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");
    private static final DateTimeFormatter ICS_DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss");

    private final CoreMapper mapper;
    private final CourseService courseService;

    public TimetableService(CoreMapper mapper, CourseService courseService) {
        this.mapper = mapper;
        this.courseService = courseService;
    }

    /** 我的课表：课程列表 + 学分统计。 */
    public Map<String, Object> view(long studentId) {
        List<Map<String, Object>> courses = courseService.enrolledCourses(studentId);
        for (Map<String, Object> c : courses) {
            long cid = Values.asLong(c.get("courseId"));
            c.put("id", cid);
            c.put("courseId", cid);
        }
        double credits = courseService.sumCredits(courses);
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("courses", courses);
        out.put("count", courses.size());
        out.put("credits", credits);
        out.put("creditLimit", courseService.creditLimit(studentId));
        out.put("semester", SEMESTER);
        out.put("weeks", WEEKS);
        return out;
    }

    /** 导出 .ics（RFC 5545）。 */
    public String exportIcs(long studentId) {
        List<Map<String, Object>> courses = courseService.enrolledCourses(studentId);
        String stamp = LocalDateTime.now().format(ICS_STAMP);
        StringBuilder sb = new StringBuilder();
        sb.append("BEGIN:VCALENDAR\r\n")
                .append("VERSION:2.0\r\n")
                .append("PRODID:-//course-platform//Course Timetable//CN\r\n")
                .append("CALSCALE:GREGORIAN\r\n")
                .append("METHOD:PUBLISH\r\n")
                .append("X-WR-CALNAME:").append(SEMESTER).append("\r\n");
        for (Map<String, Object> c : courses) {
            long cid = Values.asLong(c.get("courseId"));
            String name = Values.str(c.get("name"), "课程");
            String teacher = Values.str(c.get("teacher"), "");
            String campus = Values.str(c.get("campus"), "");
            String place = Values.str(c.get("place"), "");
            for (Map<String, Object> seg : segments(c)) {
                int day = Values.asInt(seg.get("day"));
                int start = Values.asInt(seg.get("start"));
                int end = Values.asInt(seg.get("end"));
                if (day < 1 || day > 7 || start < 1 || end < start) {
                    continue;
                }
                LocalDate date = SEMESTER_MONDAY.plusDays(day - 1L);
                String dtStart = date.atTime(periodStart(start)).format(ICS_DATE_TIME);
                String dtEnd = date.atTime(periodEnd(end)).format(ICS_DATE_TIME);
                sb.append("BEGIN:VEVENT\r\n")
                        .append("UID:course-").append(cid).append('-').append(day).append('-').append(start)
                        .append("@course-platform\r\n")
                        .append("DTSTAMP:").append(stamp).append("\r\n")
                        .append("DTSTART:").append(dtStart).append("\r\n")
                        .append("DTEND:").append(dtEnd).append("\r\n")
                        .append("RRULE:FREQ=WEEKLY;COUNT=").append(WEEKS).append("\r\n")
                        .append("SUMMARY:").append(escape(name)).append("\r\n")
                        .append("LOCATION:").append(escape((campus + " " + place).trim())).append("\r\n")
                        .append("DESCRIPTION:").append(escape("教师：" + teacher + "；校区：" + campus)).append("\r\n")
                        .append("END:VEVENT\r\n");
            }
        }
        sb.append("END:VCALENDAR\r\n");
        return sb.toString();
    }

    private List<Map<String, Object>> segments(Map<String, Object> course) {
        Object o = course.get("schedule");
        List<Map<String, Object>> out = new ArrayList<>();
        if (o instanceof List<?> list) {
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> seg = (Map<String, Object>) m;
                    out.add(seg);
                }
            }
        }
        return out;
    }

    private static LocalTime periodStart(int period) {
        return FIRST_PERIOD.plusMinutes((long) (period - 1) * (PERIOD_MINUTES + BREAK_MINUTES));
    }

    private static LocalTime periodEnd(int period) {
        return periodStart(period).plusMinutes(PERIOD_MINUTES);
    }

    private static String escape(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("\\", "\\\\").replace(";", "\\;").replace(",", "\\,").replace("\n", "\\n");
    }
}
