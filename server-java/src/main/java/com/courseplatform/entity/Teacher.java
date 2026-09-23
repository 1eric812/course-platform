package com.courseplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 教师档案 */
@TableName("teacher")
public class Teacher {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String teacherNo;
    private String title;
    private String dept;
    private String phone;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTeacherNo() { return teacherNo; }
    public void setTeacherNo(String teacherNo) { this.teacherNo = teacherNo; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDept() { return dept; }
    public void setDept(String dept) { this.dept = dept; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
