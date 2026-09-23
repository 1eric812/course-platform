package com.courseplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** 学期信息表 */
@TableName("semester")
public class Semester {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String academicYear;
    private Integer semesterNo;
    private String name;
    private Integer isCurrent;
    private LocalDate startDate;
    private LocalDate endDate;
    private LocalDateTime selectionStart;
    private LocalDateTime selectionEnd;
    private LocalDateTime dropDeadline;
    private LocalDateTime gradeDeadline;
    private Integer archived;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAcademicYear() { return academicYear; }
    public void setAcademicYear(String academicYear) { this.academicYear = academicYear; }
    public Integer getSemesterNo() { return semesterNo; }
    public void setSemesterNo(Integer semesterNo) { this.semesterNo = semesterNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getIsCurrent() { return isCurrent; }
    public void setIsCurrent(Integer isCurrent) { this.isCurrent = isCurrent; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public LocalDateTime getSelectionStart() { return selectionStart; }
    public void setSelectionStart(LocalDateTime selectionStart) { this.selectionStart = selectionStart; }
    public LocalDateTime getSelectionEnd() { return selectionEnd; }
    public void setSelectionEnd(LocalDateTime selectionEnd) { this.selectionEnd = selectionEnd; }
    public LocalDateTime getDropDeadline() { return dropDeadline; }
    public void setDropDeadline(LocalDateTime dropDeadline) { this.dropDeadline = dropDeadline; }
    public LocalDateTime getGradeDeadline() { return gradeDeadline; }
    public void setGradeDeadline(LocalDateTime gradeDeadline) { this.gradeDeadline = gradeDeadline; }
    public Integer getArchived() { return archived; }
    public void setArchived(Integer archived) { this.archived = archived; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
