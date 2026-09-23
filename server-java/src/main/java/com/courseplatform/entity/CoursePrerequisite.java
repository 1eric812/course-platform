package com.courseplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

/** 课程先修关系 */
@TableName("course_prerequisite")
public class CoursePrerequisite {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long courseId;
    private Long prereqCourseId;
    private String prereqName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCourseId() { return courseId; }
    public void setCourseId(Long courseId) { this.courseId = courseId; }
    public Long getPrereqCourseId() { return prereqCourseId; }
    public void setPrereqCourseId(Long prereqCourseId) { this.prereqCourseId = prereqCourseId; }
    public String getPrereqName() { return prereqName; }
    public void setPrereqName(String prereqName) { this.prereqName = prereqName; }
}
