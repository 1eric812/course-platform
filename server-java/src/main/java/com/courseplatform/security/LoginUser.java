package com.courseplatform.security;

import java.math.BigDecimal;

/** 登录用户上下文（由 AuthInterceptor 解析 Token + 数据库档案后写入 request） */
public class LoginUser {
    private Long userId;
    private String username;
    private String role;      // STUDENT / TEACHER / ADMIN
    private String realName;

    // 学生档案
    private Long studentId;
    private String studentNo;
    private String grade;
    private String major;
    private BigDecimal creditLimit;

    // 教师档案
    private Long teacherId;
    private String teacherNo;
    private String title;
    private String dept;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getRealName() { return realName; }
    public void setRealName(String realName) { this.realName = realName; }
    public Long getStudentId() { return studentId; }
    public void setStudentId(Long studentId) { this.studentId = studentId; }
    public String getStudentNo() { return studentNo; }
    public void setStudentNo(String studentNo) { this.studentNo = studentNo; }
    public String getGrade() { return grade; }
    public void setGrade(String grade) { this.grade = grade; }
    public String getMajor() { return major; }
    public void setMajor(String major) { this.major = major; }
    public BigDecimal getCreditLimit() { return creditLimit; }
    public void setCreditLimit(BigDecimal creditLimit) { this.creditLimit = creditLimit; }
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
    public String getTeacherNo() { return teacherNo; }
    public void setTeacherNo(String teacherNo) { this.teacherNo = teacherNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDept() { return dept; }
    public void setDept(String dept) { this.dept = dept; }
}
