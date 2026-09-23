package com.courseplatform.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.courseplatform.entity.Course;
import com.courseplatform.entity.Score;
import com.courseplatform.entity.Semester;
import com.courseplatform.entity.StudentCourse;
import com.courseplatform.exception.ApiException;
import com.courseplatform.mapper.CourseMapper;
import com.courseplatform.mapper.ScoreMapper;
import com.courseplatform.mapper.SemesterMapper;
import com.courseplatform.mapper.StudentCourseMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 学期体系：CRUD、当前学期、学期归档（成绩/选课/课程联动置档） */
@Service
public class SemesterService {

    private final SemesterMapper semesterMapper;
    private final ScoreMapper scoreMapper;
    private final StudentCourseMapper studentCourseMapper;
    private final CourseMapper courseMapper;
    private final OperationLogService operationLogService;

    public SemesterService(SemesterMapper semesterMapper, ScoreMapper scoreMapper,
                           StudentCourseMapper studentCourseMapper, CourseMapper courseMapper,
                           OperationLogService operationLogService) {
        this.semesterMapper = semesterMapper;
        this.scoreMapper = scoreMapper;
        this.studentCourseMapper = studentCourseMapper;
        this.courseMapper = courseMapper;
        this.operationLogService = operationLogService;
    }

    private Map<String, Object> toMap(Semester s) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getId());
        m.put("academicYear", s.getAcademicYear());
        m.put("semesterNo", s.getSemesterNo());
        m.put("name", s.getName());
        m.put("isCurrent", s.getIsCurrent() != null && s.getIsCurrent() == 1);
        m.put("startDate", s.getStartDate() == null ? null : s.getStartDate().toString());
        m.put("endDate", s.getEndDate() == null ? null : s.getEndDate().toString());
        m.put("selectionStart", s.getSelectionStart() == null ? null : s.getSelectionStart().toString().replace("T", " "));
        m.put("selectionEnd", s.getSelectionEnd() == null ? null : s.getSelectionEnd().toString().replace("T", " "));
        m.put("dropDeadline", s.getDropDeadline() == null ? null : s.getDropDeadline().toString().replace("T", " "));
        m.put("gradeDeadline", s.getGradeDeadline() == null ? null : s.getGradeDeadline().toString().replace("T", " "));
        m.put("archived", s.getArchived() != null && s.getArchived() == 1);
        m.put("remark", s.getRemark());
        return m;
    }

    /** 全部学期（含 is_current / archived 标记） */
    public Map<String, Object> listAll() {
        List<Semester> list = semesterMapper.selectList(new QueryWrapper<Semester>().orderByAsc("id"));
        List<Map<String, Object>> items = new ArrayList<>();
        for (Semester s : list) items.add(toMap(s));
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("items", items);
        out.put("total", items.size());
        return out;
    }

    /** 当前学期：is_current=1 优先，否则取最新一条 */
    public Semester getCurrent() {
        Semester s = semesterMapper.selectOne(new QueryWrapper<Semester>().eq("is_current", 1).last("LIMIT 1"));
        if (s == null) {
            s = semesterMapper.selectList(new QueryWrapper<Semester>().orderByDesc("id").last("LIMIT 1")).stream().findFirst().orElse(null);
        }
        return s;
    }

    private void clearCurrentFlag(Long exceptId) {
        for (Semester s : semesterMapper.selectList(new QueryWrapper<Semester>().eq("is_current", 1))) {
            if (exceptId != null && exceptId.equals(s.getId())) continue;
            s.setIsCurrent(0);
            semesterMapper.updateById(s);
        }
    }

    @Transactional
    public Map<String, Object> create(Map<String, Object> body) {
        String academicYear = str(body.get("academicYear"));
        if (academicYear.isEmpty()) throw new ApiException(400, "学年（academicYear）不能为空", null);
        Integer semesterNo = body.get("semesterNo") == null ? null : Integer.valueOf(String.valueOf(body.get("semesterNo")));
        if (semesterNo == null) throw new ApiException(400, "学期序号（semesterNo）不能为空", null);
        Semester s = new Semester();
        s.setAcademicYear(academicYear);
        s.setSemesterNo(semesterNo);
        String name = str(body.get("name"));
        if (name.isEmpty()) name = academicYear + "-" + semesterNo;
        s.setName(name);
        s.setIsCurrent(Boolean.TRUE.equals(body.get("isCurrent")) ? 1 : 0);
        s.setStartDate(parseDate(body.get("startDate")));
        s.setEndDate(parseDate(body.get("endDate")));
        s.setSelectionStart(parseDateTime(body.get("selectionStart")));
        s.setSelectionEnd(parseDateTime(body.get("selectionEnd")));
        s.setDropDeadline(parseDateTime(body.get("dropDeadline")));
        s.setGradeDeadline(parseDateTime(body.get("gradeDeadline")));
        s.setArchived(Boolean.TRUE.equals(body.get("archived")) ? 1 : 0);
        s.setRemark(str(body.get("remark")).isEmpty() ? null : str(body.get("remark")));
        s.setCreatedAt(LocalDateTime.now());
        s.setUpdatedAt(LocalDateTime.now());
        if (s.getIsCurrent() == 1) clearCurrentFlag(null);
        semesterMapper.insert(s);
        return toMap(s);
    }

    @Transactional
    public Map<String, Object> update(Long id, Map<String, Object> body) {
        Semester s = semesterMapper.selectById(id);
        if (s == null) throw new ApiException(404, "学期不存在", null);
        if (body.get("academicYear") != null) s.setAcademicYear(str(body.get("academicYear")));
        if (body.get("semesterNo") != null) s.setSemesterNo(Integer.valueOf(String.valueOf(body.get("semesterNo"))));
        if (body.get("name") != null) s.setName(str(body.get("name")));
        if (body.get("isCurrent") != null) s.setIsCurrent(Boolean.TRUE.equals(body.get("isCurrent")) ? 1 : 0);
        if (body.get("startDate") != null) s.setStartDate(parseDate(body.get("startDate")));
        if (body.get("endDate") != null) s.setEndDate(parseDate(body.get("endDate")));
        if (body.get("selectionStart") != null) s.setSelectionStart(parseDateTime(body.get("selectionStart")));
        if (body.get("selectionEnd") != null) s.setSelectionEnd(parseDateTime(body.get("selectionEnd")));
        if (body.get("dropDeadline") != null) s.setDropDeadline(parseDateTime(body.get("dropDeadline")));
        if (body.get("gradeDeadline") != null) s.setGradeDeadline(parseDateTime(body.get("gradeDeadline")));
        if (body.get("archived") != null) s.setArchived(Boolean.TRUE.equals(body.get("archived")) ? 1 : 0);
        if (body.get("remark") != null) s.setRemark(str(body.get("remark")).isEmpty() ? null : str(body.get("remark")));
        s.setUpdatedAt(LocalDateTime.now());
        if (s.getIsCurrent() != null && s.getIsCurrent() == 1) clearCurrentFlag(id);
        semesterMapper.updateById(s);
        operationLogService.write(42L, "admin", "学期更新", s.getName(), "更新学期信息（id=" + id + "）");
        return toMap(s);
    }

    /**
     * 归档学期：成绩置为「已归档」、选课记录置 ARCHIVED、课程置「已归档」、学期 archived=1，
     * 并写入操作日志。
     */
    @Transactional
    public Map<String, Object> archive(Long id) {
        Semester s = semesterMapper.selectById(id);
        if (s == null) throw new ApiException(404, "学期不存在", null);
        if (s.getArchived() != null && s.getArchived() == 1)
            throw new ApiException(400, "该学期已归档，请勿重复归档", null);
        // 成绩 → 已归档
        scoreMapper.update(null, new UpdateWrapper<Score>()
                .eq("semester_id", id)
                .in("status", "待录入", "已录入", "已审核")
                .set("status", "已归档"));
        // 选课记录 → ARCHIVED
        studentCourseMapper.update(null, new UpdateWrapper<StudentCourse>()
                .eq("semester_id", id)
                .ne("status", "ARCHIVED")
                .set("status", "ARCHIVED"));
        // 课程 → 已归档
        courseMapper.update(null, new UpdateWrapper<Course>()
                .eq("semester_id", id)
                .set("course_status", "已归档"));
        s.setArchived(1);
        s.setIsCurrent(0);
        s.setUpdatedAt(LocalDateTime.now());
        semesterMapper.updateById(s);
        operationLogService.write(42L, "admin", "学期归档", s.getName(),
                "归档学期成绩、选课记录、课程数据（semester_id=" + id + "）");
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("ok", true);
        out.put("semester", toMap(s));
        return out;
    }

    private String str(Object v) { return v == null ? "" : String.valueOf(v).trim(); }

    private LocalDate parseDate(Object v) {
        if (v == null || String.valueOf(v).isBlank()) return null;
        return LocalDate.parse(String.valueOf(v).trim().substring(0, 10));
    }

    private LocalDateTime parseDateTime(Object v) {
        if (v == null || String.valueOf(v).isBlank()) return null;
        String raw = String.valueOf(v).trim().replace('T', ' ');
        if (raw.length() <= 10) return LocalDate.parse(raw).atStartOfDay();
        return LocalDateTime.parse(raw.substring(0, 19).replace(' ', 'T'));
    }
}
