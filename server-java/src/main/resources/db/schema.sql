-- =====================================================================
-- 高校选课平台 · MySQL 表结构定义（schema.sql）
-- 目标库：course_platform（MySQL 8.0+ / InnoDB / utf8mb4）
-- 依据：course-platform/server/data.js 的 40 门课程、3 个学生账号、
--       教师档案、先修关系、教务规则、异常工单、消息等种子数据
-- 说明：本脚本只建表不插数据，模拟数据见同目录 data.sql
-- 执行方式：mysql -u root -p course_platform < schema.sql
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

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
-- 2. student 学生档案（一账号一档案）
-- ---------------------------------------------------------------------
CREATE TABLE student (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '学生主键',
    user_id      BIGINT UNSIGNED NOT NULL                COMMENT '关联 sys_user.id',
    student_no   VARCHAR(20)     NOT NULL                COMMENT '学号（兼登录名）',
    grade        VARCHAR(16)              DEFAULT NULL   COMMENT '年级，如 2023级',
    major        VARCHAR(64)              DEFAULT NULL   COMMENT '专业',
    credit_limit DECIMAL(4, 1)   NOT NULL DEFAULT 30.0   COMMENT '本学期学分上限',
    class_name   VARCHAR(64)              DEFAULT NULL   COMMENT '行政班（可空）',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted      TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_user (user_id),
    UNIQUE KEY uk_student_no (student_no),
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '学生档案表';

-- ---------------------------------------------------------------------
-- 3. teacher 教师档案（一账号一档案）
-- ---------------------------------------------------------------------
CREATE TABLE teacher (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '教师主键',
    user_id    BIGINT UNSIGNED NOT NULL                COMMENT '关联 sys_user.id',
    teacher_no VARCHAR(20)     NOT NULL                COMMENT '工号（兼登录名）',
    title      VARCHAR(32)              DEFAULT NULL   COMMENT '职称，如 副教授',
    dept       VARCHAR(64)              DEFAULT NULL   COMMENT '所属院系',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted    TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_teacher_user (user_id),
    UNIQUE KEY uk_teacher_no (teacher_no),
    CONSTRAINT fk_teacher_user FOREIGN KEY (user_id) REFERENCES sys_user (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '教师档案表';

-- ---------------------------------------------------------------------
-- 4. course 课程（40 门，含名额与先修冗余字段）
-- ---------------------------------------------------------------------
CREATE TABLE course (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '课程主键',
    code        VARCHAR(20)     NOT NULL                COMMENT '课程代码，如 CS101',
    name        VARCHAR(128)    NOT NULL                COMMENT '课程名称',
    category    VARCHAR(16)     NOT NULL                COMMENT '类别：必修 / 选修 / 通识 / 体育',
    credits     DECIMAL(3, 1)   NOT NULL                COMMENT '学分',
    teacher_id  BIGINT UNSIGNED          DEFAULT NULL   COMMENT '授课教师，关联 teacher.id',
    teacher_name VARCHAR(32)    NOT NULL                COMMENT '授课教师姓名（冗余，兼容原接口字段 teacher）',
    campus      VARCHAR(32)              DEFAULT NULL   COMMENT '开课校区',
    place       VARCHAR(64)              DEFAULT NULL   COMMENT '上课地点',
    assessment  VARCHAR(16)              DEFAULT NULL   COMMENT '考核方式：考试 / 考查 / 论文 等',
    capacity    INT             NOT NULL                COMMENT '课程容量（总名额）',
    enrolled    INT             NOT NULL DEFAULT 0      COMMENT '已选人数',
    remaining   INT GENERATED ALWAYS AS (capacity - enrolled) VIRTUAL COMMENT '剩余名额（派生列，不占存储）',
    rating      DECIMAL(2, 1)            DEFAULT NULL   COMMENT '课程评分（0.0 - 5.0）',
    prereq_name VARCHAR(128)             DEFAULT NULL   COMMENT '先修课程名称（冗余展示，明细见 course_prerequisite）',
    tags        VARCHAR(255)             DEFAULT NULL   COMMENT '标签，多值以英文逗号分隔',
    intro       VARCHAR(500)             DEFAULT NULL   COMMENT '课程简介',
    created_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    deleted     TINYINT         NOT NULL DEFAULT 0      COMMENT '逻辑删除标记',
    PRIMARY KEY (id),
    UNIQUE KEY uk_course_code (code),
    KEY idx_course_category (category),
    KEY idx_course_teacher (teacher_id),
    CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES teacher (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '课程表';

-- ---------------------------------------------------------------------
-- 5. course_schedule 排课时间段（一课多段）
-- ---------------------------------------------------------------------
CREATE TABLE course_schedule (
    id           BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '排课主键',
    course_id    BIGINT UNSIGNED NOT NULL                COMMENT '关联 course.id',
    day_of_week  TINYINT         NOT NULL                COMMENT '星期：1=周一 ... 7=周日',
    start_period TINYINT         NOT NULL                COMMENT '起始节次',
    end_period   TINYINT         NOT NULL                COMMENT '结束节次',
    created_at   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_schedule_segment (course_id, day_of_week, start_period, end_period),
    CONSTRAINT fk_schedule_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '课程排课时间段表';

-- ---------------------------------------------------------------------
-- 6. course_prerequisite 先修关系
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
-- 7. student_course 选课记录（学生 ↔ 课程）
-- ---------------------------------------------------------------------
CREATE TABLE student_course (
    id          BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    student_id  BIGINT UNSIGNED NOT NULL                COMMENT '关联 student.id',
    course_id   BIGINT UNSIGNED NOT NULL                COMMENT '关联 course.id',
    status      VARCHAR(16)     NOT NULL DEFAULT 'SELECTED' COMMENT '状态：SELECTED=已选，DROPPED=已退',
    source      VARCHAR(16)     NOT NULL DEFAULT 'SEED'     COMMENT '来源：SEED=预置，SELECTION=选课提交，ADMIN=教务调整',
    selected_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '选课时间',
    dropped_at  DATETIME                 DEFAULT NULL   COMMENT '退课时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_student_course (student_id, course_id),
    KEY idx_sc_course (course_id),
    CONSTRAINT fk_sc_student FOREIGN KEY (student_id) REFERENCES student (id) ON DELETE CASCADE,
    CONSTRAINT fk_sc_course FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '选课记录表';

-- ---------------------------------------------------------------------
-- 8. wishlist 心愿单（含志愿序）
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
-- 9. seat_subscription 名额放号订阅
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
-- 10. selection_ticket 选课受理票据（异步提交单）
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
-- 11. selection_ticket_item 受理票证明细（逐门结果）
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
-- 12. message 消息中心
-- ---------------------------------------------------------------------
CREATE TABLE message (
    id         BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
    user_id    BIGINT UNSIGNED NOT NULL                COMMENT '接收账号，关联 sys_user.id（按账号隔离）',
    type       VARCHAR(16)     NOT NULL                COMMENT '类型：result=选课结果，seat=放号提醒，system=系统通知，drop=退课通知',
    title      VARCHAR(128)    NOT NULL                COMMENT '标题',
    body       VARCHAR(500)             DEFAULT NULL   COMMENT '正文',
    is_read    TINYINT         NOT NULL DEFAULT 0      COMMENT '已读标记：0=未读，1=已读',
    created_at DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    KEY idx_message_user (user_id, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '消息中心表（按账号隔离）';

-- ---------------------------------------------------------------------
-- 13. user_preference 个人中心偏好设置
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
-- 14. selection_rule 教务选课规则（单行配置）
-- ---------------------------------------------------------------------
CREATE TABLE selection_rule (
    id                  TINYINT       NOT NULL DEFAULT 1 COMMENT '固定为 1（单行配置）',
    selection_open      TINYINT       NOT NULL DEFAULT 1 COMMENT '选课开关：1=开放，0=关闭',
    credit_limit        DECIMAL(4, 1) NOT NULL DEFAULT 30.0 COMMENT '全局学分上限',
    max_wishlist        INT           NOT NULL DEFAULT 8 COMMENT '心愿单最大志愿数',
    allow_cross_campus  TINYINT       NOT NULL DEFAULT 1 COMMENT '是否允许跨校区选课',
    block_on_conflict   TINYINT       NOT NULL DEFAULT 1 COMMENT '冲突课程是否直接拦截',
    updated_at          DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_general_ci COMMENT = '教务选课规则配置表';

-- ---------------------------------------------------------------------
-- 15. selection_period 选课阶段（预选 / 正选 / 补退选）
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
-- 16. anomaly_ticket 异常工单（教务端处理）
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
