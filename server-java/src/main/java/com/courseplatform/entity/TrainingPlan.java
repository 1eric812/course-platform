package com.courseplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 培养方案表（毕业学分标准） */
@TableName("training_plan")
public class TrainingPlan {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String major;
    private String grade;
    private Long semesterId;
    private String requiredCourses;
    private String electiveCourses;
    private BigDecimal minCredits;
    private BigDecimal maxCredits;
    private BigDecimal gradTotalCredits;
    private BigDecimal gradRequiredCredits;
    private BigDecimal gradElectiveCredits;
    private BigDecimal genEduCredits;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }
    public String getRequiredCourses() { return requiredCourses; }
    public void setRequiredCourses(String requiredCourses) { this.requiredCourses = requiredCourses; }
    public String getElectiveCourses() { return electiveCourses; }
    public void setElectiveCourses(String electiveCourses) { this.electiveCourses = electiveCourses; }
    public BigDecimal getMinCredits() { return minCredits; }
    public void setMinCredits(BigDecimal minCredits) { this.minCredits = minCredits; }
    public BigDecimal getMaxCredits() { return maxCredits; }
    public void setMaxCredits(BigDecimal maxCredits) { this.maxCredits = maxCredits; }
    public BigDecimal getGradTotalCredits() { return gradTotalCredits; }
    public void setGradTotalCredits(BigDecimal gradTotalCredits) { this.gradTotalCredits = gradTotalCredits; }
    public BigDecimal getGradRequiredCredits() { return gradRequiredCredits; }
    public void setGradRequiredCredits(BigDecimal gradRequiredCredits) { this.gradRequiredCredits = gradRequiredCredits; }
    public BigDecimal getGradElectiveCredits() { return gradElectiveCredits; }
    public void setGradElectiveCredits(BigDecimal gradElectiveCredits) { this.gradElectiveCredits = gradElectiveCredits; }
    public BigDecimal getGenEduCredits() { return genEduCredits; }
    public void setGenEduCredits(BigDecimal genEduCredits) { this.genEduCredits = genEduCredits; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
