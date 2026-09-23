package com.courseplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 学生档案（一账号一档案，含学业学分绩点字段） */
@TableName("student")
public class Student {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String studentNo;
    private String grade;
    private String college;
    private String major;
    private String className;
    private String eduSystem;
    private LocalDate enrollmentDate;
    private LocalDate graduationDate;
    private String phone;
    private String studyStatus;
    private BigDecimal creditLimit;
    private BigDecimal currentSelectedCredits;
    private BigDecimal currentEarnedCredits;
    private BigDecimal totalEarnedCredits;
    private BigDecimal requiredEarnedCredits;
    private BigDecimal electiveEarnedCredits;
    private BigDecimal genEduEarnedCredits;
    private BigDecimal failedCredits;
    private BigDecimal totalGpa;
    private BigDecimal avgGpa;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getCollege() { return college; }
    public void setCollege(String college) { this.college = college; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public String getClassName() { return className; }
    public void setClassName(String className) { this.className = className; }
    public String getEduSystem() { return eduSystem; }
    public void setEduSystem(String eduSystem) { this.eduSystem = eduSystem; }
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public void setEnrollmentDate(LocalDate enrollmentDate) { this.enrollmentDate = enrollmentDate; }
    public LocalDate getGraduationDate() { return graduationDate; }
    public void setGraduationDate(LocalDate graduationDate) { this.graduationDate = graduationDate; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getStudyStatus() { return studyStatus; }
    public void setStudyStatus(String studyStatus) { this.studyStatus = studyStatus; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    public BigDecimal getCurrentSelectedCredits() { return currentSelectedCredits; }
    public void setCurrentSelectedCredits(BigDecimal currentSelectedCredits) { this.currentSelectedCredits = currentSelectedCredits; }
    public BigDecimal getCurrentEarnedCredits() { return currentEarnedCredits; }
    public void setCurrentEarnedCredits(BigDecimal currentEarnedCredits) { this.currentEarnedCredits = currentEarnedCredits; }
    public BigDecimal getTotalEarnedCredits() { return totalEarnedCredits; }
    public void setTotalEarnedCredits(BigDecimal totalEarnedCredits) { this.totalEarnedCredits = totalEarnedCredits; }
    public BigDecimal getRequiredEarnedCredits() { return requiredEarnedCredits; }
    public void setRequiredEarnedCredits(BigDecimal requiredEarnedCredits) { this.requiredEarnedCredits = requiredEarnedCredits; }
    public BigDecimal getElectiveEarnedCredits() { return electiveEarnedCredits; }
    public void setElectiveEarnedCredits(BigDecimal electiveEarnedCredits) { this.electiveEarnedCredits = electiveEarnedCredits; }
    public BigDecimal getGenEduEarnedCredits() { return genEduEarnedCredits; }
    public void setGenEduEarnedCredits(BigDecimal genEduEarnedCredits) { this.genEduEarnedCredits = genEduEarnedCredits; }
    public BigDecimal getFailedCredits() { return failedCredits; }
    public void setFailedCredits(BigDecimal failedCredits) { this.failedCredits = failedCredits; }
    public BigDecimal getTotalGpa() { return totalGpa; }
    public void setTotalGpa(BigDecimal totalGpa) { this.totalGpa = totalGpa; }
    public BigDecimal getAvgGpa() { return avgGpa; }
    public void setAvgGpa(BigDecimal avgGpa) { this.avgGpa = avgGpa; }
}
