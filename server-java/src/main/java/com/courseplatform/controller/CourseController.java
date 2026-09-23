package com.courseplatform.controller;

import com.courseplatform.security.AuthContext;
import com.courseplatform.security.LoginUser;
import com.courseplatform.service.CourseService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 课程中心（学生端） */
@RestController
@RequestMapping("/api")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/filters")
    public Map<String, Object> filters() {
        return courseService.filterOptions();
    }

    @GetMapping("/courses")
    public Map<String, Object> courses(@RequestParam Map<String, String> q, HttpServletRequest req) {
        return courseService.listCourses(q, AuthContext.current(req));
    }

    @GetMapping("/courses/{id}")
    public Map<String, Object> detail(@PathVariable Long id, HttpServletRequest req) {
        return courseService.courseDetail(id, AuthContext.current(req));
    }
}
