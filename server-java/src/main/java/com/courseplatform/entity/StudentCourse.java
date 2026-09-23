package com.courseplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

/** 选课记录（学生 ↔ 课程，按学期隔离） */
@TableName("student_course")
public class StudentCourse {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long courseId;
    private Long semesterId;
    private String selectType; // 正常选课 / 重修选课 / 补选 / 跨选
    private String status;     // SELECTED=已选（待开课）/ STUDYING=正常修读 / DROPPED=已退课 / PENDING_SCORE=结课待录成绩 / ARCHIVED=已归档
    private String source;     // SEED / SELECTION / ADMIN
    private LocalDateTime selectedAt;
    private LocalDateTime droppedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }
    public String getSelectType() { return selectType; }
    public void setSelectType(String selectType) { this.selectType = selectType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public LocalDateTime getSelectedAt() { return selectedAt; }
    public void setSelectedAt(LocalDateTime selectedAt) { this.selectedAt = selectedAt; }
    public LocalDateTime getDroppedAt() { return droppedAt; }
    public void setDroppedAt(LocalDateTime droppedAt) { this.droppedAt = droppedAt; }
}
