package com.courseplatform;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 高校选课平台 · Java 后端启动类。
 *
 * <p>启动前请先执行 {@code src/main/resources/db/00-create-database.sql}、
 * {@code db/schema.sql}、{@code db/data.sql} 完成数据库初始化，
 * 并在 {@code application.yml} 中填写本机 MySQL 账号密码。</p>
 *
 * <p>已包含：多端（学生 / 教师 / 教务）登录鉴权、按角色接口权限控制，
 * 以及原 Node 版 37 条业务接口的兼容实现。</p>
 */
@SpringBootApplication
@MapperScan("com.courseplatform.mapper")
@EnableScheduling
public class CoursePlatformApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoursePlatformApplication.class, args);
    }
}
