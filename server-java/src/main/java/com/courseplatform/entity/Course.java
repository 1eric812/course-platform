package com.courseplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

/** 课程表 */
@TableName("course")
public class Course {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String category;
    private BigDecimal credits;
    private Long teacherId;
    private String teacherName;
    private String campus;
    private String place;
    private String assessment;
    private Integer capacity;
    private Integer enrolled;
    @TableField(exist = false)
    private Integer remaining; // 派生：capacity - enrolled
    private BigDecimal rating;
    private String prereqName;
    private String tags;
    private String intro;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public BigDecimal getCredits() { return credits; }
    public void setCredits(BigDecimal credits) { this.credits = credits; }
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public String getCampus() { return campus; }
    public void setCampus(String campus) { this.campus = campus; }
    public String getPlace() { return place; }
    public void setPlace(String place) { this.place = place; }
    public String getAssessment() { return assessment; }
    public void setAssessment(String assessment) { this.assessment = assessment; }
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    public Integer getEnrolled() { return enrolled; }
    public void setEnrolled(Integer enrolled) { this.enrolled = enrolled; }
    public Integer getRemaining() { return remaining != null ? remaining : (capacity == null ? 0 : capacity - (enrolled == null ? 0 : enrolled)); }
    public void setRemaining(Integer remaining) { this.remaining = remaining; }
    public BigDecimal getRating() { return rating; }
    public void setRating(BigDecimal rating) { this.rating = rating; }
    public String getPrereqName() { return prereqName; }
    public void setPrereqName(String prereqName) { this.prereqName = prereqName; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getIntro() { return intro; }
    public void setIntro(String intro) { this.intro = intro; }
}
