package com.courseplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 学生成绩表（含补考重修绩点） */
@TableName("score")
public class Score {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long studentId;
    private Long courseId;
    private Long semesterId;
    private BigDecimal regularScore;
    private BigDecimal attendanceScore;
    private BigDecimal homeworkScore;
    private BigDecimal midtermScore;
    private BigDecimal finalScore;
    private BigDecimal totalScore;
    private String gradeLevel;
    private Integer creditObtained;
    private BigDecimal courseGpa;
    private Integer retakeFlag;
    private BigDecimal retakeScore;
    private Integer retakeCourseFlag;
    private Integer retakeTimes;
    private String status;   // 待录入 / 已录入 / 已审核 / 已归档
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }
    public BigDecimal getRegularScore() { return regularScore; }
    public void setRegularScore(BigDecimal regularScore) { this.regularScore = regularScore; }
    public BigDecimal getAttendanceScore() { return attendanceScore; }
    public void setAttendanceScore(BigDecimal attendanceScore) { this.attendanceScore = attendanceScore; }
    public BigDecimal getHomeworkScore() { return homeworkScore; }
    public void setHomeworkScore(BigDecimal homeworkScore) { this.homeworkScore = homeworkScore; }
    public BigDecimal getMidtermScore() { return midtermScore; }
    public void setMidtermScore(BigDecimal midtermScore) { this.midtermScore = midtermScore; }
    public BigDecimal getFinalScore() { return finalScore; }
    public void setFinalScore(BigDecimal finalScore) { this.finalScore = finalScore; }
    public BigDecimal getTotalScore() { return totalScore; }
    public void setTotalScore(BigDecimal totalScore) { this.totalScore = totalScore; }
    public String getGradeLevel() { return gradeLevel; }
    public void setGradeLevel(String gradeLevel) { this.gradeLevel = gradeLevel; }
    public Integer getCreditObtained() { return creditObtained; }
    public void setCreditObtained(Integer creditObtained) { this.creditObtained = creditObtained; }
    public BigDecimal getCourseGpa() { return courseGpa; }
    public void setCourseGpa(BigDecimal courseGpa) { this.courseGpa = courseGpa; }
    public Integer getRetakeFlag() { return retakeFlag; }
    public void setRetakeFlag(Integer retakeFlag) { this.retakeFlag = retakeFlag; }
    public BigDecimal getRetakeScore() { return retakeScore; }
    public void setRetakeScore(BigDecimal retakeScore) { this.retakeScore = retakeScore; }
    public Integer getRetakeCourseFlag() { return retakeCourseFlag; }
    public void setRetakeCourseFlag(Integer retakeCourseFlag) { this.retakeCourseFlag = retakeCourseFlag; }
    public Integer getRetakeTimes() { return retakeTimes; }
    public void setRetakeTimes(Integer retakeTimes) { this.retakeTimes = retakeTimes; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
