-- =====================================================================
-- 高校选课平台 · MySQL 表结构定义（schema.sql）· 完整版大学选课系统
-- 目标库：course_platform（MySQL 8.0+ / InnoDB / utf8mb4）
-- 版本：v2.0（完整版需求：学期体系 / 成绩体系 / 学分绩点 / 课表体系 /
--       培养方案 / 重修补考 / 学期归档 / 公告日志）
-- 说明：本脚本只建表不插数据，模拟数据见同目录 data.sql
-- 执行方式：mysql -u root -p course_platform < schema.sql
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS operation_log;
DROP TABLE IF EXISTS announcement;
DROP TABLE IF EXISTS student_timetable;
DROP TABLE IF EXISTS score;
DROP TABLE IF EXISTS training_plan;
DROP TABLE IF EXISTS semester;
DROP TABLE IF EXISTS selection_ticket_item;
DROP TABLE IF EXISTS selection_ticket;
DROP TABLE IF EXISTS seat_subscription;
DROP TABLE IF EXISTS wishlist;
DROP TABLE IF EXISTS student_course;
DROP TABLE IF EXISTS course_prerequisite;
DROP TABLE IF EXISTS course_schedule;
DROP TABLE IF EXISTS message;
DROP TABLE IF EXISTS user_preference;
DROP TABLE IF EXISTS anomaly_ticket;
DROP TABLE IF EXISTS selection_period;
DROP TABLE IF EXISTS selection_rule;
DROP TABLE IF EXISTS course;
DROP TABLE IF EXISTS student;
DROP TABLE IF EXISTS teacher;
DROP TABLE IF EXISTS sys_user;

SET FOREIGN_KEY_CHECKS = 1;

-- ---------------------------------------------------------------------
-- 1. sys_user 统一账号表（学生 / 教师 / 教务共用，含密码与角色）
-- ---------------------------------------------------------------------
CREATE TABLE sys_user (
    id            BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '账号主键',
    username      VARCHAR(64)     NOT NULL                COMMENT '登录名：学生=学号，教师=工号，教务=admin',
    password      VARCHAR(100)    NOT NULL                COMMENT '密码（BCrypt 哈希，$2b$10$ 前缀）',
    role          VARCHAR(16)     NOT NULL                COMMENT '角色：STUDENT / TEACHER / ADMIN',
    real_name     VARCHAR(32)     NOT NULL                COMMENT '真实姓名（展示用）',
    avatar        VARCHAR(255)             DEFAULT NULL   COMMENT '头像地址（可空）',
    status        TINYINT         NOT NULL DEFAULT 1      COMMENT '账号状态：1=启用，0=停用',
    last_login_at DATETIME                 DEFAULT NULL   COMMENT '最近登录时间',
    created_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted       TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除：0=正常，1=已删除',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_username (username),
    KEY idx_user_role (role)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '统一账号表（多端登录身份）';

-- ---------------------------------------------------------------------
-- 2. student 学生档案（一账号一档案；含完整学业字段）
-- ---------------------------------------------------------------------
CREATE TABLE student (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '学生主键',
    user_id      BIGINT UNSIGNED NOT NULL                COMMENT '关联 sys_user.id',
    student_no   VARCHAR(20)     NOT NULL                COMMENT '学号（兼登录名）',
    grade        VARCHAR(16)              DEFAULT NULL   COMMENT '年级，如 2023级',
    college      VARCHAR(64)              DEFAULT NULL   COMMENT '学院',
    major        VARCHAR(64)              DEFAULT NULL   COMMENT '专业',
    class_name   VARCHAR(64)              DEFAULT NULL   COMMENT '行政班（可空）',
    edu_system   VARCHAR(16)              DEFAULT '四年' COMMENT '学制：四年 / 五年 等',
    enrollment_date DATE                 DEFAULT NULL   COMMENT '入学时间',
    graduation_date DATE                 DEFAULT NULL   COMMENT '毕业时间',
    phone        VARCHAR(20)              DEFAULT NULL   COMMENT '手机号',
    study_status VARCHAR(16)     NOT NULL DEFAULT '在读' COMMENT '状态：在读 / 休学 / 毕业',
    credit_limit DECIMAL(4, 1)   NOT NULL DEFAULT 30.0   COMMENT '本学期学分上限',
    -- 学业核心字段（按学期自动结算）
    current_selected_credits DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '当前学期已选学分（在读）',
    current_earned_credits   DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '当前学期已修学分（已获得）',
    total_earned_credits     DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '累计总学分（全部学期已修）',
    required_earned_credits  DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '必修已修学分',
    elective_earned_credits  DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '选修已修学分',
    gen_edu_earned_credits   DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '通识已修学分',
    failed_credits           DECIMAL(5,1) NOT NULL DEFAULT 0.0 COMMENT '不及格学分（挂科未通过）',
    total_gpa                DECIMAL(4,1) NOT NULL DEFAULT 0.0 COMMENT '总绩点（加权）',
    avg_gpa                  DECIMAL(4,2) NOT NULL DEFAULT 0.00 COMMENT '平均绩点（GPA）',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_user (user_id),
    UNIQUE KEY uk_student_no (student_no),
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '学生档案表（含学业学分绩点字段）';

-- ---------------------------------------------------------------------
-- 3. teacher 教师档案（一账号一档案）
-- ---------------------------------------------------------------------
CREATE TABLE teacher (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '教师主键',
    user_id    BIGINT UNSIGNED NOT NULL                COMMENT '关联 sys_user.id',
    teacher_no VARCHAR(20)     NOT NULL                COMMENT '工号（兼登录名）',
    title      VARCHAR(32)              DEFAULT NULL   COMMENT '职称，如 副教授',
    dept       VARCHAR(64)              DEFAULT NULL   COMMENT '所属院系',
    phone      VARCHAR(20)              DEFAULT NULL   COMMENT '联系电话',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_teacher_user (user_id),
    UNIQUE KEY uk_teacher_no (teacher_no),
    CONSTRAINT fk_teacher_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '教师档案表';

-- ---------------------------------------------------------------------
-- 4. semester 学期信息表（大学系统必备，新增）
-- ---------------------------------------------------------------------
CREATE TABLE semester (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '学期主键',
    academic_year  VARCHAR(16)     NOT NULL                COMMENT '学年名称，如 2026-2027',
    semester_no    TINYINT         NOT NULL                COMMENT '学期序号：1=第一学期，2=第二学期',
    name           VARCHAR(32)     NOT NULL                COMMENT '学期名称，如 2026-2027-1',
    is_current     TINYINT         NOT NULL DEFAULT 0      COMMENT '是否当前学期：1=是，0=否',
    start_date     DATE            NOT NULL                COMMENT '学期开始日期',
    end_date       DATE            NOT NULL                COMMENT '学期结束日期',
    selection_start DATETIME       NOT NULL                COMMENT '选课开启时间',
    selection_end   DATETIME       NOT NULL                COMMENT '选课关闭时间',
    drop_deadline   DATETIME       NOT NULL                COMMENT '退课截止时间',
    grade_deadline  DATETIME       NOT NULL                COMMENT '成绩录入截止时间',
    archived       TINYINT         NOT NULL DEFAULT 0      COMMENT '是否已归档：0=未归档，1=已归档（归档后不可改）',
    remark         VARCHAR(255)             DEFAULT NULL   COMMENT '备注',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_semester_name (name),
    KEY idx_semester_current (is_current, archived)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '学期信息表';

-- ---------------------------------------------------------------------
-- 5. course 课程（40 门基础 + 完整大学课程信息字段）
-- ---------------------------------------------------------------------
CREATE TABLE course (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '课程主键',
    code        VARCHAR(20)     NOT NULL                COMMENT '课程代码，如 CS101',
    name        VARCHAR(128)    NOT NULL                COMMENT '课程名称',
    category    VARCHAR(16)     NOT NULL                COMMENT '课程大类：必修课 / 专业限选 / 通识必修 / 通识选修 / 实践课 / 体育课 / 创新创业课（兼容旧值 必修/选修/通识/体育）',
    course_attr VARCHAR(16)     NOT NULL DEFAULT '主修课' COMMENT '课程属性：主修课 / 辅修课 / 重修课 / 跨专业选课',
    credits     DECIMAL(3, 1)   NOT NULL                COMMENT '学分',
    total_hours INT             NOT NULL DEFAULT 0      COMMENT '总学时',
    theory_hours INT            NOT NULL DEFAULT 0      COMMENT '理论学时',
    practice_hours INT          NOT NULL DEFAULT 0      COMMENT '实践学时',
    lab_hours   INT             NOT NULL DEFAULT 0      COMMENT '实验学时',
    teacher_id  BIGINT UNSIGNED          DEFAULT NULL   COMMENT '授课教师，关联 teacher.id',
    teacher_name VARCHAR(32)    NOT NULL                COMMENT '授课教师姓名（冗余，兼容原接口字段 teacher）',
    semester_id BIGINT UNSIGNED          DEFAULT NULL   COMMENT '授课学期，关联 semester.id',
    weeks       VARCHAR(64)              DEFAULT NULL   COMMENT '上课周次，如 1-16周 / 1-8周,10-16周',
    campus      VARCHAR(32)              DEFAULT NULL   COMMENT '开课校区',
    place       VARCHAR(64)              DEFAULT NULL   COMMENT '上课地点',
    assessment  VARCHAR(16)              DEFAULT NULL   COMMENT '考核方式：考试 / 考查 / 论文 等（兼容旧值）',
    capacity    INT             NOT NULL                COMMENT '课程容量（总名额）',
    enrolled    INT             NOT NULL DEFAULT 0      COMMENT '已选人数',
    remaining   INT GENERATED ALWAYS AS (capacity - enrolled) VIRTUAL COMMENT '剩余名额（派生列，不占存储）',
    rating      DECIMAL(2, 1)            DEFAULT NULL   COMMENT '课程评分（0.0 - 5.0）',
    prereq_name VARCHAR(128)             DEFAULT NULL   COMMENT '先修课程名称（冗余展示，明细见 course_prerequisite）',
    tags        VARCHAR(255)             DEFAULT NULL   COMMENT '标签，多值以英文逗号分隔',
    -- 选课限制（大学完整版）
    limit_grade     VARCHAR(64)          DEFAULT NULL   COMMENT '限选年级，多值逗号分隔，如 2023级,2024级',
    limit_major     VARCHAR(255)         DEFAULT NULL   COMMENT '限选专业，多值逗号分隔',
    limit_class     VARCHAR(255)         DEFAULT NULL   COMMENT '限选班级，多值逗号分隔',
    prereq_required TINYINT       NOT NULL DEFAULT 0    COMMENT '是否需要前置课程：1=是',
    allow_retake    TINYINT       NOT NULL DEFAULT 1    COMMENT '是否允许重修选课：1=允许',
    allow_cross_major TINYINT     NOT NULL DEFAULT 0    COMMENT '是否允许跨专业选课：1=允许',
    -- 课程详情介绍（完整版）
    intro          VARCHAR(1000)          DEFAULT NULL   COMMENT '课程简介',
    syllabus       TEXT                              COMMENT '课程大纲',
    assessment_detail VARCHAR(255)        DEFAULT NULL   COMMENT '考核方式明细，如 平时30%+期末70%',
    open_college   VARCHAR(64)              DEFAULT NULL COMMENT '开课学院',
    applicable_major VARCHAR(255)           DEFAULT NULL COMMENT '适用专业',
    prereq_requirement VARCHAR(255)         DEFAULT NULL COMMENT '先修要求说明',
    course_objective   TEXT                             COMMENT '课程目标',
    textbook       VARCHAR(500)             DEFAULT NULL COMMENT '参考教材',
    course_status  VARCHAR(16)     NOT NULL DEFAULT '未开课' COMMENT '课程状态：未开课 / 选课中 / 开课中 / 已结课 / 已归档',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_code (code),
    KEY idx_course_category (category),
    KEY idx_course_teacher (teacher_id),
    KEY idx_course_semester (semester_id),
    KEY idx_course_status (course_status),
    CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES teacher (id) ON DELETE SET NULL,
    CONSTRAINT fk_course_semester FOREIGN KEY (semester_id) REFERENCES semester (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '课程表（完整大学课程信息）';

-- ---------------------------------------------------------------------
-- 6. course_schedule 排课时间段（一课多段，按学期隔离）
-- ---------------------------------------------------------------------
CREATE TABLE course_schedule (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '排课主键',
    course_id    BIGINT UNSIGNED NOT NULL                COMMENT '关联 course.id',
    semester_id  BIGINT UNSIGNED          DEFAULT NULL   COMMENT '授课学期，关联 semester.id',
    day_of_week  TINYINT         NOT NULL                COMMENT '星期：1=周一 ... 7=周日',
    start_period TINYINT         NOT NULL                COMMENT '起始节次',
    end_period   TINYINT         NOT NULL                COMMENT '结束节次',
    place        VARCHAR(64)              DEFAULT NULL   COMMENT '上课教室（冗余，兼容课表展示）',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_schedule_segment (course_id, day_of_week, start_period, end_period),
    KEY idx_schedule_semester (semester_id),
    CONSTRAINT fk_schedule_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE,
    CONSTRAINT fk_schedule_semester FOREIGN KEY (semester_id) REFERENCES semester (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '课程排课时间段表';

-- ---------------------------------------------------------------------
-- 7. course_prerequisite 先修关系
-- ---------------------------------------------------------------------
CREATE TABLE course_prerequisite (
    id               BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    course_id        BIGINT UNSIGNED NOT NULL                COMMENT '后续课程，关联 course.id',
    prereq_course_id BIGINT UNSIGNED          DEFAULT NULL   COMMENT '先修课程 id（未在本校课程表内时为空）',
    prereq_name      VARCHAR(128)    NOT NULL                COMMENT '先修课程名称',
    created_at       DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_prereq_pair (course_id, prereq_name),
    CONSTRAINT fk_prereq_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE,
    CONSTRAINT fk_prereq_target FOREIGN KEY (prereq_course_id) REFERENCES course (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '课程先修关系表';

-- ---------------------------------------------------------------------
-- 8. student_course 选课记录（学生 ↔ 课程，按学期隔离）
-- ---------------------------------------------------------------------
CREATE TABLE student_course (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    student_id  BIGINT UNSIGNED NOT NULL                COMMENT '关联 student.id',
    course_id   BIGINT UNSIGNED NOT NULL                COMMENT '关联 course.id',
    semester_id BIGINT UNSIGNED          DEFAULT NULL   COMMENT '选课学期，关联 semester.id',
    select_type VARCHAR(16)     NOT NULL DEFAULT '正常选课' COMMENT '选课类型：正常选课 / 重修选课 / 补选 / 跨选',
    status      VARCHAR(16)     NOT NULL DEFAULT 'SELECTED' COMMENT '状态：SELECTED=已选（待开课），STUDYING=正常修读，DROPPED=已退课，PENDING_SCORE=结课待录成绩，ARCHIVED=已归档',
    source      VARCHAR(16)     NOT NULL DEFAULT 'SEED'     COMMENT '来源：SEED=预置，SELECTION=选课提交，ADMIN=教务调整',
    selected_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
    dropped_at  DATETIME                 DEFAULT NULL   COMMENT '退课时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_course (student_id, course_id),
    KEY idx_sc_course (course_id),
    KEY idx_sc_semester (semester_id),
    KEY idx_sc_status (student_id, status),
    CONSTRAINT fk_sc_student FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE CASCADE,
    CONSTRAINT fk_sc_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE,
    CONSTRAINT fk_sc_semester FOREIGN KEY (semester_id) REFERENCES semester (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '选课记录表';

-- ---------------------------------------------------------------------
-- 9. score 学生成绩表（大学核心，新增）
-- ---------------------------------------------------------------------
CREATE TABLE score (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '成绩主键',
    student_id     BIGINT UNSIGNED NOT NULL                COMMENT '关联 student.id',
    course_id      BIGINT UNSIGNED NOT NULL                COMMENT '关联 course.id',
    semester_id    BIGINT UNSIGNED NOT NULL                COMMENT '关联 semester.id',
    -- 分项成绩
    regular_score  DECIMAL(5,1)             DEFAULT NULL   COMMENT '平时成绩',
    attendance_score DECIMAL(5,1)           DEFAULT NULL   COMMENT '考勤成绩',
    homework_score DECIMAL(5,1)             DEFAULT NULL   COMMENT '作业成绩',
    midterm_score  DECIMAL(5,1)             DEFAULT NULL   COMMENT '期中成绩',
    final_score    DECIMAL(5,1)             DEFAULT NULL   COMMENT '期末成绩',
    total_score    DECIMAL(5,1)             DEFAULT NULL   COMMENT '综合总成绩（最终分数）',
    grade_level    VARCHAR(8)               DEFAULT NULL   COMMENT '成绩等级：优 / 良 / 中 / 及格 / 不及格',
    credit_obtained TINYINT      NOT NULL DEFAULT 0      COMMENT '学分获得状态：0=未获得，1=已获得',
    course_gpa     DECIMAL(3,1)             DEFAULT NULL   COMMENT '单课绩点（0.0 - 4.0）',
    -- 补考 / 重修
    retake_flag    TINYINT       NOT NULL DEFAULT 0      COMMENT '是否补考：0=否，1=是',
    retake_score   DECIMAL(5,1)             DEFAULT NULL   COMMENT '补考成绩',
    retake_course_flag TINYINT   NOT NULL DEFAULT 0      COMMENT '是否重修：0=否，1=是',
    retake_times   INT           NOT NULL DEFAULT 0      COMMENT '重修次数',
    -- 状态
    status         VARCHAR(16)   NOT NULL DEFAULT '待录入' COMMENT '成绩状态：待录入 / 已录入 / 已审核 / 已归档',
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_score_student_course (student_id, course_id, semester_id),
    KEY idx_score_semester (semester_id, status),
    KEY idx_score_student (student_id, semester_id),
    KEY idx_score_course (course_id),
    CONSTRAINT fk_score_student FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE CASCADE,
    CONSTRAINT fk_score_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE,
    CONSTRAINT fk_score_semester FOREIGN KEY (semester_id) REFERENCES semester (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '学生成绩表（含补考重修绩点）';

-- ---------------------------------------------------------------------
-- 10. student_timetable 学生课表数据表（按学期生成与归档，新增）
-- ---------------------------------------------------------------------
CREATE TABLE student_timetable (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '课表主键',
    student_id   BIGINT UNSIGNED NOT NULL                COMMENT '关联 student.id',
    semester_id  BIGINT UNSIGNED NOT NULL                COMMENT '关联 semester.id',
    course_id    BIGINT UNSIGNED NOT NULL                COMMENT '关联 course.id',
    week_pattern VARCHAR(64)              DEFAULT NULL   COMMENT '周次排布，如 1-16周',
    day_of_week  TINYINT         NOT NULL                COMMENT '星期：1=周一 ... 7=周日',
    start_period TINYINT         NOT NULL                COMMENT '开始节次',
    end_period   TINYINT         NOT NULL                COMMENT '结束节次',
    place        VARCHAR(64)              DEFAULT NULL   COMMENT '教室',
    teacher_id   BIGINT UNSIGNED          DEFAULT NULL   COMMENT '授课教师，关联 teacher.id',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_timetable_segment (student_id, semester_id, day_of_week, start_period),
    KEY idx_timetable_student (student_id, semester_id),
    KEY idx_timetable_course (course_id),
    CONSTRAINT fk_timetable_student FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE CASCADE,
    CONSTRAINT fk_timetable_semester FOREIGN KEY (semester_id) REFERENCES semester (id) ON DELETE CASCADE,
    CONSTRAINT fk_timetable_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE,
    CONSTRAINT fk_timetable_teacher FOREIGN KEY (teacher_id) REFERENCES teacher (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '学生课表数据表（按学期生成与归档）';

-- ---------------------------------------------------------------------
-- 11. training_plan 培养方案表（大学真实必备，新增）
-- ---------------------------------------------------------------------
CREATE TABLE training_plan (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '培养方案主键',
    major          VARCHAR(64)     NOT NULL                COMMENT '专业',
    grade          VARCHAR(16)     NOT NULL                COMMENT '年级，如 2023级',
    semester_id    BIGINT UNSIGNED NOT NULL                COMMENT '关联学期 semester.id',
    required_courses VARCHAR(1000)          DEFAULT NULL   COMMENT '本专业本学期必须修读课程ID，逗号分隔',
    elective_courses VARCHAR(1000)          DEFAULT NULL   COMMENT '本专业本学期可选修课程ID，逗号分隔',
    min_credits    DECIMAL(4,1)   NOT NULL DEFAULT 0.0    COMMENT '每学期最低学分',
    max_credits    DECIMAL(4,1)   NOT NULL DEFAULT 30.0   COMMENT '每学期最高学分',
    grad_total_credits DECIMAL(5,1) NOT NULL DEFAULT 120.0 COMMENT '毕业所需总学分',
    grad_required_credits DECIMAL(5,1) NOT NULL DEFAULT 80.0 COMMENT '毕业必修学分',
    grad_elective_credits DECIMAL(5,1) NOT NULL DEFAULT 30.0 COMMENT '毕业选修学分',
    gen_edu_credits DECIMAL(5,1)  NOT NULL DEFAULT 10.0   COMMENT '通识学分要求',
    remark         VARCHAR(255)             DEFAULT NULL   COMMENT '备注',
    created_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_plan (major, grade, semester_id),
    CONSTRAINT fk_plan_semester FOREIGN KEY (semester_id) REFERENCES semester (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '培养方案表（毕业学分标准）';

-- ---------------------------------------------------------------------
-- 12. wishlist 心愿单（含志愿序）
-- ---------------------------------------------------------------------
CREATE TABLE wishlist (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    student_id BIGINT UNSIGNED NOT NULL                COMMENT '关联 student.id',
    course_id  BIGINT UNSIGNED NOT NULL                COMMENT '关联 course.id',
    priority   INT             NOT NULL DEFAULT 1      COMMENT '志愿序（1 为第一志愿）',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '加入时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_wishlist_pair (student_id, course_id),
    KEY idx_wishlist_priority (student_id, priority),
    CONSTRAINT fk_wishlist_student FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE CASCADE,
    CONSTRAINT fk_wishlist_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '心愿单表';

-- ---------------------------------------------------------------------
-- 13. seat_subscription 名额放号订阅
-- ---------------------------------------------------------------------
CREATE TABLE seat_subscription (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    student_id BIGINT UNSIGNED NOT NULL                COMMENT '关联 student.id',
    course_id  BIGINT UNSIGNED NOT NULL                COMMENT '关联 course.id',
    status     VARCHAR(16)     NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE / CANCELLED / NOTIFIED',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '订阅时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_subscription_pair (student_id, course_id),
    KEY idx_subscription_course (course_id),
    CONSTRAINT fk_seat_sub_student FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE CASCADE,
    CONSTRAINT fk_seat_sub_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '名额放号订阅表';

-- ---------------------------------------------------------------------
-- 14. selection_ticket 选课受理票据（异步提交单）
-- ---------------------------------------------------------------------
CREATE TABLE selection_ticket (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_no      VARCHAR(32)     NOT NULL                COMMENT '受理单号，如 T-1726…',
    student_id     BIGINT UNSIGNED NOT NULL                COMMENT '关联 student.id',
    status         VARCHAR(16)     NOT NULL DEFAULT 'ACCEPTED' COMMENT '状态：ACCEPTED=已受理，PROCESSING=处理中，SUCCESS=全部成功，PARTIAL=部分成功，FAILED=全部失败',
    queue_position INT                      DEFAULT NULL   COMMENT '排队序号',
    total_count    INT             NOT NULL DEFAULT 0      COMMENT '提交课程总数',
    accepted_count INT             NOT NULL DEFAULT 0      COMMENT '成功门数',
    rejected_count INT             NOT NULL DEFAULT 0      COMMENT '失败门数',
    submitted_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
    finished_at    DATETIME                 DEFAULT NULL   COMMENT '处理完成时间',
    deleted        TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_ticket_no (ticket_no),
    KEY idx_ticket_student (student_id, submitted_at),
    CONSTRAINT fk_ticket_student FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '选课受理票据表';

-- ---------------------------------------------------------------------
-- 15. selection_ticket_item 受理票证明细（逐门结果）
-- ---------------------------------------------------------------------
CREATE TABLE selection_ticket_item (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    ticket_id   BIGINT UNSIGNED NOT NULL                COMMENT '关联 selection_ticket.id',
    course_id   BIGINT UNSIGNED NOT NULL                COMMENT '关联 course.id',
    course_name VARCHAR(128)    NOT NULL                COMMENT '课程名称（冗余，便于结果展示）',
    result      VARCHAR(16)     NOT NULL                COMMENT '结果：ACCEPTED / REJECTED',
    reason      VARCHAR(255)             DEFAULT NULL   COMMENT '失败原因（容量已满 / 时间冲突 / 学分超限 等）',
    PRIMARY KEY (id),
    KEY idx_item_ticket (ticket_id),
    CONSTRAINT fk_item_ticket FOREIGN KEY (ticket_id) REFERENCES selection_ticket (id) ON DELETE CASCADE,
    CONSTRAINT fk_item_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '选课受理票证明细表';

-- ---------------------------------------------------------------------
-- 16. message 消息中心
-- ---------------------------------------------------------------------
CREATE TABLE message (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT UNSIGNED NOT NULL                COMMENT '接收账号，关联 sys_user.id（按账号隔离）',
    type       VARCHAR(16)     NOT NULL                COMMENT '类型：result=选课结果，seat=放号提醒，system=系统通知，drop=退课通知，grade=成绩通知',
    title      VARCHAR(128)    NOT NULL                COMMENT '标题',
    body       VARCHAR(500)             DEFAULT NULL   COMMENT '正文',
    is_read    TINYINT         NOT NULL DEFAULT 0      COMMENT '已读标记：0=未读，1=已读',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_message_user (user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '消息中心表（按账号隔离）';

-- ---------------------------------------------------------------------
-- 17. user_preference 个人中心偏好设置
-- ---------------------------------------------------------------------
CREATE TABLE user_preference (
    id             BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id        BIGINT UNSIGNED NOT NULL                COMMENT '关联 sys_user.id',
    theme          VARCHAR(16)     NOT NULL DEFAULT 'light'      COMMENT '主题：light / dark',
    density        VARCHAR(16)     NOT NULL DEFAULT 'standard'   COMMENT '信息密度：standard / compact',
    font_scale     VARCHAR(16)     NOT NULL DEFAULT 'standard'   COMMENT '字体档位：small / standard / large / xlarge',
    timetable_view VARCHAR(16)     NOT NULL DEFAULT 'week'        COMMENT '课表视图：week / list',
    compact_filter TINYINT         NOT NULL DEFAULT 0             COMMENT '筛选区紧凑模式：1=是',
    notify_result  TINYINT         NOT NULL DEFAULT 1             COMMENT '接收选课结果通知',
    notify_seat    TINYINT         NOT NULL DEFAULT 1             COMMENT '接收放号提醒',
    notify_system  TINYINT         NOT NULL DEFAULT 1             COMMENT '接收系统通知',
    notify_drop    TINYINT         NOT NULL DEFAULT 1             COMMENT '接收退课通知',
    updated_at     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_pref_user (user_id),
    CONSTRAINT fk_pref_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '账号偏好设置表';

-- ---------------------------------------------------------------------
-- 18. selection_rule 教务选课规则（单行配置，完整版）
-- ---------------------------------------------------------------------
CREATE TABLE selection_rule (
    id                  TINYINT       NOT NULL DEFAULT 1 COMMENT '固定为 1（单行配置）',
    selection_open      TINYINT       NOT NULL DEFAULT 1 COMMENT '选课开关：1=开放，0=关闭',
    credit_limit        DECIMAL(4, 1) NOT NULL DEFAULT 30.0 COMMENT '单学期学分上限',
    max_courses         INT           NOT NULL DEFAULT 10 COMMENT '单学期最大课程数',
    max_wishlist        INT           NOT NULL DEFAULT 8 COMMENT '心愿单最大志愿数',
    drop_deadline       DATETIME               DEFAULT NULL COMMENT '退课截止时间',
    check_conflict      TINYINT       NOT NULL DEFAULT 1 COMMENT '是否开启冲突检测：1=开',
    check_prereq        TINYINT       NOT NULL DEFAULT 1 COMMENT '是否开启前置课检测：1=开',
    check_grade_major   TINYINT       NOT NULL DEFAULT 1 COMMENT '是否开启年级专业限制：1=开',
    allow_retake        TINYINT       NOT NULL DEFAULT 1 COMMENT '是否开放重修选课：1=开',
    allow_cross_major   TINYINT       NOT NULL DEFAULT 0 COMMENT '是否开放跨专业选课：1=开',
    allow_cross_campus  TINYINT       NOT NULL DEFAULT 1 COMMENT '是否允许跨校区选课',
    block_on_conflict   TINYINT       NOT NULL DEFAULT 1 COMMENT '冲突课程是否直接拦截',
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '教务选课规则配置表（完整版）';

-- ---------------------------------------------------------------------
-- 19. selection_period 选课阶段（预选 / 正选 / 补退选）
-- ---------------------------------------------------------------------
CREATE TABLE selection_period (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    phase_code VARCHAR(16)     NOT NULL                COMMENT '阶段编码：PRE / MAIN / ADJUST',
    phase_name VARCHAR(32)     NOT NULL                COMMENT '阶段名称：预选 / 正选 / 补退选',
    start_time DATETIME        NOT NULL                COMMENT '开始时间',
    end_time   DATETIME        NOT NULL                COMMENT '结束时间',
    status     VARCHAR(16)     NOT NULL DEFAULT 'NOT_STARTED' COMMENT '状态：NOT_STARTED / ONGOING / FINISHED',
    remark     VARCHAR(128)             DEFAULT NULL   COMMENT '说明',
    PRIMARY KEY (id),
    UNIQUE KEY uk_period_code (phase_code)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '选课阶段时间表';

-- ---------------------------------------------------------------------
-- 20. anomaly_ticket 异常工单（教务端处理）
-- ---------------------------------------------------------------------
CREATE TABLE anomaly_ticket (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    type         VARCHAR(16)     NOT NULL                COMMENT '类型：conflict=冲突，full=满额，session=会话，credit=学分',
    student_no   VARCHAR(20)              DEFAULT NULL   COMMENT '学生学号',
    student_name VARCHAR(32)              DEFAULT NULL   COMMENT '学生姓名',
    course_id    BIGINT UNSIGNED          DEFAULT NULL   COMMENT '关联 course.id（可空）',
    course_name  VARCHAR(128)             DEFAULT NULL   COMMENT '课程名称',
    reason       VARCHAR(255)             DEFAULT NULL   COMMENT '工单描述',
    status       VARCHAR(16)     NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING=待处理，PROCESSING=处理中，RESOLVED=已解决',
    resolution   VARCHAR(255)             DEFAULT NULL   COMMENT '处理结果说明',
    handled_by   VARCHAR(32)              DEFAULT NULL   COMMENT '处理人',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    handled_at   DATETIME                 DEFAULT NULL   COMMENT '处理时间',
    PRIMARY KEY (id),
    KEY idx_anomaly_status (status, created_at),
    CONSTRAINT fk_anomaly_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '异常工单表';

-- ---------------------------------------------------------------------
-- 21. announcement 公告表（教务发布，新增）
-- ---------------------------------------------------------------------
CREATE TABLE announcement (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '公告主键',
    title      VARCHAR(128)    NOT NULL                COMMENT '公告标题',
    content    TEXT            NOT NULL                COMMENT '公告内容',
    publisher_id BIGINT UNSIGNED       DEFAULT NULL     COMMENT '发布人，关联 sys_user.id',
    is_pinned  TINYINT         NOT NULL DEFAULT 0      COMMENT '是否置顶：1=是，0=否',
    status     VARCHAR(16)     NOT NULL DEFAULT 'PUBLISHED' COMMENT '状态：DRAFT=草稿，PUBLISHED=已发布，OFFLINE=已下线',
    published_at DATETIME               DEFAULT NULL   COMMENT '发布时间',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_announcement_status (status, published_at),
    CONSTRAINT fk_announcement_publisher FOREIGN KEY (publisher_id) REFERENCES sys_user (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '公告表';

-- ---------------------------------------------------------------------
-- 22. operation_log 操作日志表（教务审计，新增）
-- ---------------------------------------------------------------------
CREATE TABLE operation_log (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '日志主键',
    user_id    BIGINT UNSIGNED          DEFAULT NULL   COMMENT '操作账号，关联 sys_user.id（可空）',
    username   VARCHAR(64)              DEFAULT NULL   COMMENT '登录名（冗余）',
    action     VARCHAR(64)     NOT NULL                COMMENT '操作动作，如 学期归档 / 成绩审核 / 课程导入',
    target     VARCHAR(128)             DEFAULT NULL   COMMENT '操作对象描述',
    detail     VARCHAR(500)             DEFAULT NULL   COMMENT '操作详情',
    ip         VARCHAR(64)              DEFAULT NULL   COMMENT '来源 IP',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_log_user (user_id, created_at),
    KEY idx_log_action (action, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '操作日志表（审计）';
