package com.courseplatform.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.math.BigDecimal;

/** 课程表（完整大学课程信息） */
@TableName("course")
public class Course {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String category;
    private String courseAttr;
    private BigDecimal credits;
    private Integer totalHours;
    private Integer theoryHours;
    private Integer practiceHours;
    private Integer labHours;
    private Long teacherId;
    private String teacherName;
    private Long semesterId;
    private String weeks;
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
    private String limitGrade;
    private String limitMajor;
    private String limitClass;
    private Integer prereqRequired;
    private Integer allowRetake;
    private Integer allowCrossMajor;
    private String intro;
    private String syllabus;
    private String assessmentDetail;
    private String openCollege;
    private String applicableMajor;
    private String prereqRequirement;
    private String courseObjective;
    private String textbook;
    private String courseStatus;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    public String getCourseAttr() { return courseAttr; }
    public void setCourseAttr(String courseAttr) { this.courseAttr = courseAttr; }
    public BigDecimal getCredits() { return credits; }
    public void setCredits(BigDecimal credits) { this.credits = credits; }
    public Integer getTotalHours() { return totalHours; }
    public void setTotalHours(Integer totalHours) { this.totalHours = totalHours; }
    public Integer getTheoryHours() { return theoryHours; }
    public void setTheoryHours(Integer theoryHours) { this.theoryHours = theoryHours; }
    public Integer getPracticeHours() { return practiceHours; }
    public void setPracticeHours(Integer practiceHours) { this.practiceHours = practiceHours; }
    public Integer getLabHours() { return labHours; }
    public void setLabHours(Integer labHours) { this.labHours = labHours; }
    public Long getTeacherId() { return teacherId; }
    public void setTeacherId(Long teacherId) { this.teacherId = teacherId; }
    public String getTeacherName() { return teacherName; }
    public void setTeacherName(String teacherName) { this.teacherName = teacherName; }
    public Long getSemesterId() { return semesterId; }
    public void setSemesterId(Long semesterId) { this.semesterId = semesterId; }
    public String getWeeks() { return weeks; }
    public void setWeeks(String weeks) { this.weeks = weeks; }
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
    public String getLimitGrade() { return limitGrade; }
    public void setLimitGrade(String limitGrade) { this.limitGrade = limitGrade; }
    public String getLimitMajor() { return limitMajor; }
    public void setLimitMajor(String limitMajor) { this.limitMajor = limitMajor; }
    public String getLimitClass() { return limitClass; }
    public void setLimitClass(String limitClass) { this.limitClass = limitClass; }
    public Integer getPrereqRequired() { return prereqRequired; }
    public void setPrereqRequired(Integer prereqRequired) { this.prereqRequired = prereqRequired; }
    public Integer getAllowRetake() { return allowRetake; }
    public void setAllowRetake(Integer allowRetake) { this.allowRetake = allowRetake; }
    public Integer getAllowCrossMajor() { return allowCrossMajor; }
    public void setAllowCrossMajor(Integer allowCrossMajor) { this.allowCrossMajor = allowCrossMajor; }
    public String getIntro() { return intro; }
    public void setIntro(String intro) { this.intro = intro; }
    public String getSyllabus() { return syllabus; }
    public void setSyllabus(String syllabus) { this.syllabus = syllabus; }
    public String getAssessmentDetail() { return assessmentDetail; }
    public void setAssessmentDetail(String assessmentDetail) { this.assessmentDetail = assessmentDetail; }
    public String getOpenCollege() { return openCollege; }
    public void setOpenCollege(String openCollege) { this.openCollege = openCollege; }
    public String getApplicableMajor() { return applicableMajor; }
    public void setApplicableMajor(String applicableMajor) { this.applicableMajor = applicableMajor; }
    public String getPrereqRequirement() { return prereqRequirement; }
    public void setPrereqRequirement(String prereqRequirement) { this.prereqRequirement = prereqRequirement; }
    public String getCourseObjective() { return courseObjective; }
    public void setCourseObjective(String courseObjective) { this.courseObjective = courseObjective; }
    public String getTextbook() { return textbook; }
    public void setTextbook(String textbook) { this.textbook = textbook; }
    public String getCourseStatus() { return courseStatus; }
    public void setCourseStatus(String courseStatus) { this.courseStatus = courseStatus; }
}
