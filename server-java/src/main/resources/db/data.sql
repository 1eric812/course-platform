-- =====================================================================
-- 高校选课平台 · 模拟数据初始化脚本（data.sql）
-- 依赖：请先执行 00-create-database.sql 与 schema.sql
-- 依据：course-platform/server/data.js（SEED 40 门课程 / ACCOUNT_SEEDS 3 个学生账号 /
--       PREREQ 9 条先修 / WELCOME 3 条消息 / adminState 规则与异常工单）
-- 执行：mysql -u root -p course_platform < data.sql
-- 初始化登录密码：学生 123456　教师 teacher123　教务 admin123（均为 BCrypt 哈希存储）
-- =====================================================================

SET NAMES utf8mb4;
START TRANSACTION;

-- ---------- 1. 账号表 sys_user（id 1-3 学生，4-41 教师，42 教务） ----------
INSERT INTO sys_user (id, username, password, role, real_name, status, created_at) VALUES
  (1, '2023010101', '$2b$10$5rtyyxg/6QHtLwfQARqYwePp6I5pixNb4/fWTBKfhlmU8iYfbYjG.', 'STUDENT', '林同学', 1, '2026-09-01 08:00:00'),
  (2, '2023010102', '$2b$10$5rtyyxg/6QHtLwfQARqYwePp6I5pixNb4/fWTBKfhlmU8iYfbYjG.', 'STUDENT', '苏同学', 1, '2026-09-01 08:00:00'),
  (3, '2023010103', '$2b$10$5rtyyxg/6QHtLwfQARqYwePp6I5pixNb4/fWTBKfhlmU8iYfbYjG.', 'STUDENT', '何同学', 1, '2026-09-01 08:00:00'),
  (4, 'T1001', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '张伟', 1, '2026-09-01 08:00:00'),
  (5, 'T1002', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '李静', 1, '2026-09-01 08:00:00'),
  (6, 'T1003', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '王强', 1, '2026-09-01 08:00:00'),
  (7, 'T1004', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '陈磊', 1, '2026-09-01 08:00:00'),
  (8, 'T1005', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '刘洋', 1, '2026-09-01 08:00:00'),
  (9, 'T1006', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '赵敏', 1, '2026-09-01 08:00:00'),
  (10, 'T1007', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '孙涛', 1, '2026-09-01 08:00:00'),
  (11, 'T1008', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '周华', 1, '2026-09-01 08:00:00'),
  (12, 'T1009', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '吴迪', 1, '2026-09-01 08:00:00'),
  (13, 'T1010', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '郑凯', 1, '2026-09-01 08:00:00'),
  (14, 'T1011', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '冯琳', 1, '2026-09-01 08:00:00'),
  (15, 'T1012', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '何军', 1, '2026-09-01 08:00:00'),
  (16, 'T1013', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '许静', 1, '2026-09-01 08:00:00'),
  (17, 'T1014', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '范平', 1, '2026-09-01 08:00:00'),
  (18, 'T1015', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '曹宇', 1, '2026-09-01 08:00:00'),
  (19, 'T1016', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '姜涛', 1, '2026-09-01 08:00:00'),
  (20, 'T1017', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '谢明', 1, '2026-09-01 08:00:00'),
  (21, 'T1018', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '孔亮', 1, '2026-09-01 08:00:00'),
  (22, 'T1019', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '华强', 1, '2026-09-01 08:00:00'),
  (23, 'T1020', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', 'Emily Chen', 1, '2026-09-01 08:00:00'),
  (24, 'T1021', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', 'John Smith', 1, '2026-09-01 08:00:00'),
  (25, 'T1022', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '山田樱', 1, '2026-09-01 08:00:00'),
  (26, 'T1023', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', 'Lucie Martin', 1, '2026-09-01 08:00:00'),
  (27, 'T1024', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '沈从文', 1, '2026-09-01 08:00:00'),
  (28, 'T1025', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '顾准', 1, '2026-09-01 08:00:00'),
  (29, 'T1026', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '林风眠', 1, '2026-09-01 08:00:00'),
  (30, 'T1027', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '冼星海', 1, '2026-09-01 08:00:00'),
  (31, 'T1028', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '潘菽', 1, '2026-09-01 08:00:00'),
  (32, 'T1029', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '厉以宁', 1, '2026-09-01 08:00:00'),
  (33, 'T1030', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '费孝通', 1, '2026-09-01 08:00:00'),
  (34, 'T1031', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '曲格平', 1, '2026-09-01 08:00:00'),
  (35, 'T1032', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '金岳霖', 1, '2026-09-01 08:00:00'),
  (36, 'T1033', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '姚明', 1, '2026-09-01 08:00:00'),
  (37, 'T1034', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '林丹', 1, '2026-09-01 08:00:00'),
  (38, 'T1035', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '张蕙兰', 1, '2026-09-01 08:00:00'),
  (39, 'T1036', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '孙杨', 1, '2026-09-01 08:00:00'),
  (40, 'T1037', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '马龙', 1, '2026-09-01 08:00:00'),
  (41, 'T1038', '$2b$10$Y0GrRSFsalDEc561cHhuUOpQy3a4HupgbuJy8ed1w62BtRD/Ob..6', 'TEACHER', '陈小旺', 1, '2026-09-01 08:00:00'),
  (42, 'admin', '$2b$10$N.PzVDck.jUAb3ZjVD26DueT4uB.6QiMfpgESD8YKB9rTuz6vilEC', 'ADMIN', '教务处管理员', 1, '2026-09-01 08:00:00');

-- ---------- 2. 学生档案 student ----------
INSERT INTO student (id, user_id, student_no, grade, major, credit_limit) VALUES
  (1, 1, '2023010101', '2023 级', '计算机科学与技术', 30.0),
  (2, 2, '2023010102', '2024 级', '视觉传达设计', 30.0),
  (3, 3, '2023010103', '2023 级', '数学与应用数学', 30.0);

-- ---------- 3. 教师档案 teacher（工号 T1001 起；仅演示教师「刘洋」有完整职称/院系，其余为登录账号基础档案） ----------
INSERT INTO teacher (id, user_id, teacher_no, title, dept) VALUES
  (1, 4, 'T1001', NULL, NULL),
  (2, 5, 'T1002', NULL, NULL),
  (3, 6, 'T1003', NULL, NULL),
  (4, 7, 'T1004', NULL, NULL),
  (5, 8, 'T1005', '副教授', '计算机科学与技术学院'),
  (6, 9, 'T1006', NULL, NULL),
  (7, 10, 'T1007', NULL, NULL),
  (8, 11, 'T1008', NULL, NULL),
  (9, 12, 'T1009', NULL, NULL),
  (10, 13, 'T1010', NULL, NULL),
  (11, 14, 'T1011', NULL, NULL),
  (12, 15, 'T1012', NULL, NULL),
  (13, 16, 'T1013', NULL, NULL),
  (14, 17, 'T1014', NULL, NULL),
  (15, 18, 'T1015', NULL, NULL),
  (16, 19, 'T1016', NULL, NULL),
  (17, 20, 'T1017', NULL, NULL),
  (18, 21, 'T1018', NULL, NULL),
  (19, 22, 'T1019', NULL, NULL),
  (20, 23, 'T1020', NULL, NULL),
  (21, 24, 'T1021', NULL, NULL),
  (22, 25, 'T1022', NULL, NULL),
  (23, 26, 'T1023', NULL, NULL),
  (24, 27, 'T1024', NULL, NULL),
  (25, 28, 'T1025', NULL, NULL),
  (26, 29, 'T1026', NULL, NULL),
  (27, 30, 'T1027', NULL, NULL),
  (28, 31, 'T1028', NULL, NULL),
  (29, 32, 'T1029', NULL, NULL),
  (30, 33, 'T1030', NULL, NULL),
  (31, 34, 'T1031', NULL, NULL),
  (32, 35, 'T1032', NULL, NULL),
  (33, 36, 'T1033', NULL, NULL),
  (34, 37, 'T1034', NULL, NULL),
  (35, 38, 'T1035', NULL, NULL),
  (36, 39, 'T1036', NULL, NULL),
  (37, 40, 'T1037', NULL, NULL),
  (38, 41, 'T1038', NULL, NULL);

-- ---------- 4. 课程 course（40 门，enrolled 复现原 data.js 固定伪随机值） ----------
-- 说明：enrolled 复现原 data.js 的固定伪随机算法（seed=20260920），与原实现数值一致；
--       其中 PE102「羽毛球」调整为满额 36/36，以对齐“名额已满”异常工单与放号订阅演示场景。
INSERT INTO course (id, code, name, category, credits, teacher_id, teacher_name, campus, place, assessment, capacity, enrolled, rating, prereq_name, tags, intro) VALUES
  (1, 'CS101', '数据结构与算法', '必修', 4.0, 1, '张伟', '中心校区', '信息楼 A301', '考试', 120, 36, 4.8, NULL, NULL, '数据结构与算法：由张伟老师授课，4 学分，考试考核。系统介绍数据结构与算法的核心概念、方法与实践应用。'),
  (2, 'CS102', '操作系统原理', '必修', 3.0, 2, '李静', '中心校区', '信息楼 A402', '考试', 100, 52, 4.5, NULL, NULL, '操作系统原理：由李静老师授课，3 学分，考试考核。系统介绍操作系统原理的核心概念、方法与实践应用。'),
  (3, 'CS103', '计算机网络', '必修', 3.0, 3, '王强', '中心校区', '信息楼 B205', '考试', 100, 51, 4.2, NULL, NULL, '计算机网络：由王强老师授课，3 学分，考试考核。系统介绍计算机网络的核心概念、方法与实践应用。'),
  (4, 'CS104', '数据库系统', '必修', 3.0, 4, '陈磊', '中心校区', '信息楼 B301', '考试', 110, 74, 4.6, NULL, NULL, '数据库系统：由陈磊老师授课，3 学分，考试考核。系统介绍数据库系统的核心概念、方法与实践应用。'),
  (5, 'CS201', '机器学习导论', '选修', 3.0, 5, '刘洋', '东校区', '理科楼 C101', '论文', 80, 77, 4.9, '线性代数', '名额紧张', '机器学习导论：由刘洋老师授课，3 学分，论文考核。系统介绍机器学习导论的核心概念、方法与实践应用。'),
  (6, 'CS202', '计算机图形学', '选修', 2.0, 6, '赵敏', '东校区', '理科楼 C203', '考查', 60, 34, 4.1, NULL, NULL, '计算机图形学：由赵敏老师授课，2 学分，考查考核。系统介绍计算机图形学的核心概念、方法与实践应用。'),
  (7, 'CS203', '软件工程', '选修', 3.0, 7, '孙涛', '东校区', '理科楼 C305', '论文', 70, 41, 4.3, '数据结构与算法', NULL, '软件工程：由孙涛老师授课，3 学分，论文考核。系统介绍软件工程的核心概念、方法与实践应用。'),
  (8, 'CS204', '编译原理', '选修', 3.0, 8, '周华', '中心校区', '信息楼 A505', '考试', 50, 31, 3.9, '数据结构与算法', NULL, '编译原理：由周华老师授课，3 学分，考试考核。系统介绍编译原理的核心概念、方法与实践应用。'),
  (9, 'CS205', '分布式系统', '选修', 3.0, 9, '吴迪', '中心校区', '信息楼 A601', '论文', 45, 38, 4.7, NULL, '名额紧张', '分布式系统：由吴迪老师授课，3 学分，论文考核。系统介绍分布式系统的核心概念、方法与实践应用。'),
  (10, 'CS206', '信息安全基础', '选修', 2.0, 10, '郑凯', '东校区', '理科楼 D102', '考查', 65, 61, 4.0, NULL, '名额紧张', '信息安全基础：由郑凯老师授课，2 学分，考查考核。系统介绍信息安全基础的核心概念、方法与实践应用。'),
  (11, 'CS207', '人机交互设计', '选修', 2.0, 11, '冯琳', '东校区', '理科楼 D204', '考查', 55, 37, 4.6, NULL, NULL, '人机交互设计：由冯琳老师授课，2 学分，考查考核。系统介绍人机交互设计的核心概念、方法与实践应用。'),
  (12, 'CS208', '自然语言处理', '选修', 3.0, 12, '何军', '中心校区', '信息楼 B508', '论文', 40, 31, 4.8, '机器学习导论', NULL, '自然语言处理：由何军老师授课，3 学分，论文考核。系统介绍自然语言处理的核心概念、方法与实践应用。'),
  (13, 'CS209', '深度学习进阶', '选修', 3.0, 5, '刘洋', '中心校区', '信息楼 A702', '论文', 45, 26, 4.9, '机器学习导论', NULL, '深度学习进阶：由刘洋老师授课，3 学分，论文考核。系统介绍深度学习进阶的核心概念、方法与实践应用。'),
  (14, 'CS210', '强化学习', '选修', 2.0, 5, '刘洋', '东校区', '理科楼 C401', '考查', 40, 27, 4.8, '机器学习导论', NULL, '强化学习：由刘洋老师授课，2 学分，考查考核。系统介绍强化学习的核心概念、方法与实践应用。'),
  (15, 'MA101', '高等数学（下）', '必修', 5.0, 13, '许静', '中心校区', '教学楼 101', '考试', 200, 147, 4.0, NULL, NULL, '高等数学（下）：由许静老师授课，5 学分，考试考核。系统介绍高等数学（下）的核心概念、方法与实践应用。'),
  (16, 'MA102', '线性代数', '必修', 3.0, 14, '范平', '中心校区', '教学楼 203', '考试', 180, 97, 4.4, NULL, NULL, '线性代数：由范平老师授课，3 学分，考试考核。系统介绍线性代数的核心概念、方法与实践应用。'),
  (17, 'MA103', '概率论与数理统计', '必修', 3.0, 15, '曹宇', '中心校区', '教学楼 305', '考试', 160, 77, 4.2, NULL, NULL, '概率论与数理统计：由曹宇老师授课，3 学分，考试考核。系统介绍概率论与数理统计的核心概念、方法与实践应用。'),
  (18, 'MA201', '离散数学', '选修', 3.0, 16, '姜涛', '东校区', '理科楼 A102', '考试', 90, 41, 4.1, '高等数学（下）', NULL, '离散数学：由姜涛老师授课，3 学分，考试考核。系统介绍离散数学的核心概念、方法与实践应用。'),
  (19, 'MA202', '数值分析', '选修', 2.0, 17, '谢明', '东校区', '理科楼 A204', '考查', 70, 48, 3.8, '概率论与数理统计', NULL, '数值分析：由谢明老师授课，2 学分，考查考核。系统介绍数值分析的核心概念、方法与实践应用。'),
  (20, 'PH101', '大学物理（下）', '必修', 4.0, 18, '孔亮', '中心校区', '实验楼 101', '考试', 150, 136, 3.7, NULL, NULL, '大学物理（下）：由孔亮老师授课，4 学分，考试考核。系统介绍大学物理（下）的核心概念、方法与实践应用。'),
  (21, 'PH102', '大学物理实验', '必修', 1.0, 19, '华强', '中心校区', '实验楼 205', '考查', 150, 78, 4.3, '大学物理（下）', NULL, '大学物理实验：由华强老师授课，1 学分，考查考核。系统介绍大学物理实验的核心概念、方法与实践应用。'),
  (22, 'EN101', '学术英语写作', '必修', 2.0, 20, 'Emily Chen', '中心校区', '外语楼 301', '考查', 120, 115, 4.5, NULL, '名额紧张', '学术英语写作：由Emily Chen老师授课，2 学分，考查考核。系统介绍学术英语写作的核心概念、方法与实践应用。'),
  (23, 'EN102', '英语口语与演讲', '必修', 2.0, 21, 'John Smith', '中心校区', '外语楼 402', '考查', 100, 58, 4.7, NULL, NULL, '英语口语与演讲：由John Smith老师授课，2 学分，考查考核。系统介绍英语口语与演讲的核心概念、方法与实践应用。'),
  (24, 'EN201', '第二外语（日语）', '通识', 2.0, 22, '山田樱', '东校区', '外语楼 105', '考查', 60, 26, 4.6, NULL, NULL, '第二外语（日语）：由山田樱老师授课，2 学分，考查考核。系统介绍第二外语（日语）的核心概念、方法与实践应用。'),
  (25, 'EN202', '第二外语（法语）', '通识', 2.0, 23, 'Lucie Martin', '东校区', '外语楼 107', '考查', 45, 11, 4.4, NULL, NULL, '第二外语（法语）：由Lucie Martin老师授课，2 学分，考查考核。系统介绍第二外语（法语）的核心概念、方法与实践应用。'),
  (26, 'GE201', '中国古代文学', '通识', 2.0, 24, '沈从文', '中心校区', '文法楼 201', '论文', 100, 69, 4.8, NULL, NULL, '中国古代文学：由沈从文老师授课，2 学分，论文考核。系统介绍中国古代文学的核心概念、方法与实践应用。'),
  (27, 'GE202', '西方哲学史', '通识', 2.0, 25, '顾准', '中心校区', '文法楼 303', '论文', 90, 31, 4.5, NULL, NULL, '西方哲学史：由顾准老师授课，2 学分，论文考核。系统介绍西方哲学史的核心概念、方法与实践应用。'),
  (28, 'GE203', '艺术鉴赏', '通识', 2.0, 26, '林风眠', '东校区', '艺术楼 101', '考查', 110, 89, 4.9, NULL, NULL, '艺术鉴赏：由林风眠老师授课，2 学分，考查考核。系统介绍艺术鉴赏的核心概念、方法与实践应用。'),
  (29, 'GE204', '音乐基础', '通识', 2.0, 27, '冼星海', '东校区', '艺术楼 203', '考查', 80, 57, 4.7, NULL, NULL, '音乐基础：由冼星海老师授课，2 学分，考查考核。系统介绍音乐基础的核心概念、方法与实践应用。'),
  (30, 'GE205', '心理学导论', '通识', 2.0, 28, '潘菽', '中心校区', '文法楼 405', '论文', 130, 74, 4.6, NULL, NULL, '心理学导论：由潘菽老师授课，2 学分，论文考核。系统介绍心理学导论的核心概念、方法与实践应用。'),
  (31, 'GE206', '经济学原理', '通识', 3.0, 29, '厉以宁', '中心校区', '经管楼 101', '考试', 140, 120, 4.3, NULL, NULL, '经济学原理：由厉以宁老师授课，3 学分，考试考核。系统介绍经济学原理的核心概念、方法与实践应用。'),
  (32, 'GE207', '社会学导论', '通识', 2.0, 30, '费孝通', '中心校区', '文法楼 502', '论文', 100, 66, 4.2, NULL, NULL, '社会学导论：由费孝通老师授课，2 学分，论文考核。系统介绍社会学导论的核心概念、方法与实践应用。'),
  (33, 'GE208', '环境与可持续发展', '通识', 2.0, 31, '曲格平', '东校区', '环科楼 101', '考查', 95, 28, 4.0, NULL, NULL, '环境与可持续发展：由曲格平老师授课，2 学分，考查考核。系统介绍环境与可持续发展的核心概念、方法与实践应用。'),
  (34, 'GE209', '逻辑与批判性思维', '通识', 2.0, 32, '金岳霖', '中心校区', '文法楼 601', '论文', 85, 41, 4.4, NULL, NULL, '逻辑与批判性思维：由金岳霖老师授课，2 学分，论文考核。系统介绍逻辑与批判性思维的核心概念、方法与实践应用。'),
  (35, 'PE101', '篮球', '体育', 1.0, 33, '姚明', '中心校区', '体育馆', '考查', 40, 38, 4.8, NULL, '名额紧张', '篮球：由姚明老师授课，1 学分，考查考核。系统介绍篮球的核心概念、方法与实践应用。'),
  (36, 'PE102', '羽毛球', '体育', 1.0, 34, '林丹', '中心校区', '体育馆', '考查', 36, 36, 4.9, NULL, NULL, '羽毛球：由林丹老师授课，1 学分，考查考核。系统介绍羽毛球的核心概念、方法与实践应用。'),
  (37, 'PE103', '瑜伽', '体育', 1.0, 35, '张蕙兰', '东校区', '体操房', '考查', 30, 22, 4.7, NULL, '名额紧张', '瑜伽：由张蕙兰老师授课，1 学分，考查考核。系统介绍瑜伽的核心概念、方法与实践应用。'),
  (38, 'PE104', '游泳', '体育', 1.0, 36, '孙杨', '中心校区', '游泳馆', '考查', 32, 27, 4.9, NULL, '名额紧张', '游泳：由孙杨老师授课，1 学分，考查考核。系统介绍游泳的核心概念、方法与实践应用。'),
  (39, 'PE105', '乒乓球', '体育', 1.0, 37, '马龙', '东校区', '球类馆', '考查', 30, 16, 4.6, NULL, NULL, '乒乓球：由马龙老师授课，1 学分，考查考核。系统介绍乒乓球的核心概念、方法与实践应用。'),
  (40, 'PE106', '太极', '体育', 1.0, 38, '陈小旺', '中心校区', '武术馆', '考查', 28, 21, 4.5, NULL, '名额紧张', '太极：由陈小旺老师授课，1 学分，考查考核。系统介绍太极的核心概念、方法与实践应用。');

-- ---------- 5. 排课 course_schedule（解析原 schedule 字段，如 1:1-2/3:3-4） ----------
INSERT INTO course_schedule (course_id, day_of_week, start_period, end_period) VALUES
  (1, 1, 1, 2),
  (1, 3, 3, 4),
  (2, 2, 1, 2),
  (2, 4, 6, 7),
  (3, 3, 1, 2),
  (3, 5, 3, 4),
  (4, 1, 6, 7),
  (4, 4, 1, 2),
  (5, 2, 3, 4),
  (6, 4, 3, 4),
  (7, 5, 1, 2),
  (8, 3, 6, 7),
  (8, 5, 6, 7),
  (9, 2, 6, 7),
  (10, 1, 3, 4),
  (11, 4, 8, 9),
  (12, 5, 3, 4),
  (13, 3, 8, 9),
  (14, 5, 8, 9),
  (15, 1, 1, 2),
  (15, 3, 1, 2),
  (15, 5, 1, 2),
  (16, 2, 3, 4),
  (16, 4, 3, 4),
  (17, 1, 8, 9),
  (17, 3, 8, 9),
  (18, 2, 8, 9),
  (19, 4, 1, 2),
  (20, 2, 6, 7),
  (20, 5, 6, 7),
  (21, 3, 10, 12),
  (22, 1, 3, 4),
  (23, 3, 3, 4),
  (24, 4, 6, 7),
  (25, 5, 8, 9),
  (26, 1, 6, 7),
  (27, 2, 1, 2),
  (28, 3, 6, 7),
  (29, 4, 3, 4),
  (30, 1, 8, 9),
  (31, 5, 1, 2),
  (32, 2, 8, 9),
  (33, 3, 8, 9),
  (34, 5, 6, 7),
  (35, 2, 10, 11),
  (36, 3, 10, 11),
  (37, 1, 10, 11),
  (38, 4, 10, 11),
  (39, 5, 10, 11),
  (40, 2, 3, 4);
-- 合计 50 个时间段

-- ---------- 6. 先修关系 course_prerequisite ----------
INSERT INTO course_prerequisite (course_id, prereq_course_id, prereq_name) VALUES
  (5, 16, '线性代数'),
  (7, 1, '数据结构与算法'),
  (8, 1, '数据结构与算法'),
  (12, 5, '机器学习导论'),
  (13, 5, '机器学习导论'),
  (14, 5, '机器学习导论'),
  (18, 15, '高等数学（下）'),
  (19, 17, '概率论与数理统计'),
  (21, 20, '大学物理（下）');

-- ---------- 7. 选课记录 student_course（对应 ACCOUNT_SEEDS.enrolledIds） ----------
INSERT INTO student_course (student_id, course_id, status, source, selected_at, dropped_at) VALUES
  (1, 1, 'SELECTED', 'SEED', '2026-09-19 10:00:00', NULL),
  (1, 14, 'SELECTED', 'SEED', '2026-09-19 10:00:00', NULL),
  (1, 20, 'SELECTED', 'SEED', '2026-09-19 10:00:00', NULL),
  (2, 15, 'SELECTED', 'SEED', '2026-09-19 14:32:00', NULL),
  (2, 31, 'SELECTED', 'SEED', '2026-09-19 14:32:00', NULL);
-- 说明：a1 已选 CS101(4) + CS210(2) + PH101(4) = 10 学分；a2 已选 MA101(5) + GE206(3) = 8 学分；a3 尚无选课记录

-- ---------- 8. 心愿单 wishlist（原 data.js 初始为空；此处补充 3 条演示数据，便于验证 I-03 心愿单与志愿序） ----------
INSERT INTO wishlist (student_id, course_id, priority, created_at) VALUES
  (1, 12, 1, '2026-09-20 10:12:00'),   -- CS208 自然语言处理（第一志愿）
  (1, 26, 2, '2026-09-20 10:15:00'),   -- GE201 中国古代文学
  (1, 36, 3, '2026-09-20 10:18:00');   -- PE102 羽毛球

-- ---------- 9. 放号订阅 seat_subscription（原 data.js 初始为空；此处补充 1 条演示数据，便于验证 I-04 放号提醒） ----------
INSERT INTO seat_subscription (student_id, course_id, status, created_at) VALUES
  (1, 36, 'ACTIVE', '2026-09-20 10:20:00');   -- 林同学订阅「羽毛球」放号通知

-- ---------- 10. 消息中心 message（学生 3 账号各 3 条 WELCOME + 教师刘洋 2 条） ----------
INSERT INTO message (id, user_id, type, title, body, is_read, created_at) VALUES
  (1, 1, 'system', '2026-2027 学年第一学期选课即将开始', '正式选课将于 9 月 25 日 09:00 开放，建议提前加入心愿单。', 0, '2026-09-20 09:00:00'),
  (2, 1, 'result', '高等数学（下）选课成功', '你已成功选中「高等数学（下）」，请核对课表。', 0, '2026-09-19 14:32:00'),
  (3, 1, 'seat', '机器学习导论剩余名额变化', '该课程剩余名额由 12 变为 6，请关注。', 1, '2026-09-19 11:08:00'),
  (4, 2, 'system', '2026-2027 学年第一学期选课即将开始', '正式选课将于 9 月 25 日 09:00 开放，建议提前加入心愿单。', 0, '2026-09-20 09:00:00'),
  (5, 2, 'result', '高等数学（下）选课成功', '你已成功选中「高等数学（下）」，请核对课表。', 0, '2026-09-19 14:32:00'),
  (6, 2, 'seat', '机器学习导论剩余名额变化', '该课程剩余名额由 12 变为 6，请关注。', 1, '2026-09-19 11:08:00'),
  (7, 3, 'system', '2026-2027 学年第一学期选课即将开始', '正式选课将于 9 月 25 日 09:00 开放，建议提前加入心愿单。', 0, '2026-09-20 09:00:00'),
  (8, 3, 'result', '高等数学（下）选课成功', '你已成功选中「高等数学（下）」，请核对课表。', 0, '2026-09-19 14:32:00'),
  (9, 3, 'seat', '机器学习导论剩余名额变化', '该课程剩余名额由 12 变为 6，请关注。', 1, '2026-09-19 11:08:00'),
  (10, 8, 'system', '2026-2027 学年第一学期教师端已开放', '您本学期共开设 3 门课程（机器学习导论、深度学习进阶、强化学习），可在「我的课程」查看选课名单。', 0, '2026-09-20 09:05:00'),
  (11, 8, 'seat', '「机器学习导论」选课人数提醒', '您的课程「机器学习导论」当前已选 77 人，剩余名额 3，请合理安排教室容量。', 0, '2026-09-21 08:30:00');

-- ---------- 11. 偏好设置 user_preference（与 data.js DEFAULT_PREFS 一致，为全部账号建立默认行） ----------
INSERT INTO user_preference (user_id, theme, density, font_scale, timetable_view, compact_filter, notify_result, notify_seat, notify_system, notify_drop)
SELECT id, 'light', 'standard', 'standard', 'week', 0, 1, 1, 1, 1 FROM sys_user;

-- ---------- 12. 教务规则 selection_rule（单行，对应 data.js adminState.rules） ----------
INSERT INTO selection_rule (id, selection_open, credit_limit, max_wishlist, allow_cross_campus, block_on_conflict) VALUES
  (1, 1, 30.0, 8, 1, 1);

-- ---------- 13. 选课阶段 selection_period（P-01-1 阶段名称与倒计时；正式选课按消息约定 9 月 25 日 09:00 开放） ----------
INSERT INTO selection_period (phase_code, phase_name, start_time, end_time, status, remark) VALUES
  ('PRE',    '预选',   '2026-09-18 09:00:00', '2026-09-20 23:59:59', 'FINISHED',    '仅可加入心愿单，不做名额占用'),
  ('MAIN',   '正选',   '2026-09-21 09:00:00', '2026-09-28 23:59:59', 'ONGOING',     '按志愿序批量受理，先到先得'),
  ('ADJUST', '补退选', '2026-10-08 09:00:00', '2026-10-12 23:59:59', 'NOT_STARTED', '可退课与补选，逾期不再受理');

-- ---------- 14. 异常工单 anomaly_ticket（对应 data.js adminState.anomalies 3 条） ----------
INSERT INTO anomaly_ticket (id, type, student_no, student_name, course_id, course_name, reason, status, created_at) VALUES
  (1, 'conflict', '202314254', '韩雨桐', 5,  '机器学习导论', '与「线性代数」时间冲突（周二 第 3-4 节）', 'PENDING', '2026-09-21 09:12:00'),
  (2, 'full',     '202315301', '王子轩', 36, '羽毛球',       '名额已满（0/36）', 'PENDING', '2026-09-21 09:03:00'),
  (3, 'session',  '202312089', '李浩然', NULL, NULL,          '检测到同一浏览器多账号会话异常（疑似会话串号）', 'PENDING', '2026-09-21 08:47:00');

-- ---------- 15. 受理票据 selection_ticket / selection_ticket_item（1 条历史成功单，供课表与消息核对） ----------
INSERT INTO selection_ticket (id, ticket_no, student_id, status, queue_position, total_count, accepted_count, rejected_count, submitted_at, finished_at) VALUES
  (1, 'T-20260919-1001', 2, 'SUCCESS', NULL, 1, 1, 0, '2026-09-19 14:32:00', '2026-09-19 14:32:02');
INSERT INTO selection_ticket_item (ticket_id, course_id, course_name, result, reason) VALUES
  (1, 15, '高等数学（下）', 'ACCEPTED', NULL);

-- =====================================================================
-- v2.0 完整版新增数据：学期 / 成绩 / 培养方案 / 公告 / 日志 / 字段补齐
-- =====================================================================

-- ---------- 16. 学期信息 semester（2 个历史归档学期 + 1 个当前学期） ----------
INSERT INTO semester (id, academic_year, semester_no, name, is_current, start_date, end_date, selection_start, selection_end, drop_deadline, grade_deadline, archived, remark) VALUES
  (1, '2025-2026', 1, '2025-2026-1', 0, '2025-09-01', '2026-01-15', '2025-09-05 09:00:00', '2025-09-20 23:59:59', '2025-10-15 23:59:59', '2026-01-20 23:59:59', 1, '历史学期，已归档'),
  (2, '2025-2026', 2, '2025-2026-2', 0, '2026-02-23', '2026-07-10', '2026-02-27 09:00:00', '2026-03-15 23:59:59', '2026-04-10 23:59:59', '2026-07-15 23:59:59', 1, '历史学期，已归档'),
  (3, '2026-2027', 1, '2026-2027-1', 1, '2026-09-01', '2027-01-15', '2026-09-25 09:00:00', '2026-09-28 23:59:59', '2026-10-12 23:59:59', '2027-01-20 23:59:59', 0, '当前学期');

-- ---------- 17. 补齐 student 学业字段（学院/学制/入学/学业学分绩点，模拟真实大三大四学生） ----------
UPDATE student SET
  college = '信息工程学院', edu_system = '四年', enrollment_date = '2023-09-01', graduation_date = '2027-06-30', phone = '13800000001', study_status = '在读',
  current_selected_credits = 10.0, current_earned_credits = 0.0, total_earned_credits = 92.0,
  required_earned_credits = 62.0, elective_earned_credits = 22.0, gen_edu_earned_credits = 8.0,
  failed_credits = 2.0, total_gpa = 3.2, avg_gpa = 3.2
WHERE id = 1;
UPDATE student SET
  college = '艺术设计学院', edu_system = '四年', enrollment_date = '2024-09-01', graduation_date = '2028-06-30', phone = '13800000002', study_status = '在读',
  current_selected_credits = 8.0, current_earned_credits = 0.0, total_earned_credits = 46.0,
  required_earned_credits = 30.0, elective_earned_credits = 12.0, gen_edu_earned_credits = 4.0,
  failed_credits = 0.0, total_gpa = 3.6, avg_gpa = 3.6
WHERE id = 2;
UPDATE student SET
  college = '理学院', edu_system = '四年', enrollment_date = '2023-09-01', graduation_date = '2027-06-30', phone = '13800000003', study_status = '在读',
  current_selected_credits = 0.0, current_earned_credits = 0.0, total_earned_credits = 84.0,
  required_earned_credits = 58.0, elective_earned_credits = 18.0, gen_edu_earned_credits = 8.0,
  failed_credits = 1.0, total_gpa = 3.0, avg_gpa = 3.0
WHERE id = 3;

-- ---------- 18. 补齐 course 完整字段（学时/周次/学期/课程属性/状态/详情） ----------
UPDATE course SET
  semester_id = 3, weeks = '1-16周', course_status = '选课中',
  total_hours = credits * 16, theory_hours = credits * 12, practice_hours = credits * 3, lab_hours = credits * 1,
  course_attr = CASE WHEN category IN ('必修','通识必修','实践课','体育课') THEN '主修课' WHEN category = '体育' THEN '主修课' ELSE '主修课' END,
  assessment_detail = CASE WHEN assessment = '考试' THEN '平时30% + 期末70%' WHEN assessment = '论文' THEN '平时40% + 论文60%' WHEN assessment = '考查' THEN '平时50% + 考查50%' ELSE '平时50% + 期末50%' END,
  open_college = '教务处',
  syllabus = CONCAT('《', name, '》课程大纲：第一章 课程导论；第二章 核心概念；第三章 原理与方法；第四章 综合应用；第五章 课程总结与考核说明。'),
  course_objective = CONCAT('通过本课程学习，学生应掌握《', name, '》的基本理论、方法与工具，具备独立分析与解决相关实际问题的能力。'),
  textbook = CONCAT('《', name, '》（第2版），高等教育出版社'),
  prereq_requirement = CASE WHEN prereq_name IS NULL THEN '无' ELSE CONCAT('需先修：', prereq_name) END,
  applicable_major = CASE WHEN category IN ('必修') THEN '本专业全部学生' WHEN category IN ('通识') THEN '全校各专业' ELSE '相关专业学生' END;
UPDATE course SET category = '通识必修' WHERE category = '通识';
UPDATE course SET category = '实践课' WHERE code IN ('PH102','PH101');
UPDATE course SET category = '体育课' WHERE category = '体育';

-- ---------- 19. 补齐 course_schedule 学期与教室 ----------
UPDATE course_schedule cs JOIN course c ON cs.course_id = c.id SET cs.semester_id = 3, cs.place = c.place;

-- ---------- 20. 补齐 student_course 学期与选课类型 ----------
UPDATE student_course SET semester_id = 3, select_type = '正常选课', status = 'STUDYING';

-- ---------- 21. 补齐 selection_rule 完整版字段 ----------
UPDATE selection_rule SET
  max_courses = 10, drop_deadline = '2026-10-12 23:59:59',
  check_conflict = 1, check_prereq = 1, check_grade_major = 1,
  allow_retake = 1, allow_cross_major = 0;

-- ---------- 22. 学生成绩 score（历史学期归档成绩 + 当前学期待录入占位） ----------
-- 学生1（2023级 计算机）历史成绩：2025-2026-1 / 2025-2026-2
INSERT INTO score (student_id, course_id, semester_id, regular_score, attendance_score, homework_score, midterm_score, final_score, total_score, grade_level, credit_obtained, course_gpa, retake_flag, retake_course_flag, status) VALUES
  (1, 15, 1, 88, 90, 85, 82, 86, 86.0, '良', 1, 3.5, 0, 0, '已归档'),   -- 高等数学（下）
  (1, 16, 1, 90, 92, 88, 85, 89, 89.0, '优', 1, 4.0, 0, 0, '已归档'),   -- 线性代数
  (1, 1, 2, 78, 80, 75, 70, 76, 76.0, '中', 1, 2.0, 1, 0, '已归档'),    -- 数据结构与算法（曾补考）
  (1, 17, 2, 60, 65, 62, 55, 58, 58.0, '不及格', 0, 0.0, 0, 1, '已归档'), -- 概率论（挂科重修中）
  (1, 26, 2, 92, 90, 88, 0, 0, 90.0, '优', 1, 4.0, 0, 0, '已归档');     -- 中国古代文学（考查课）
-- 学生2（2024级 视觉传达）历史成绩
INSERT INTO score (student_id, course_id, semester_id, regular_score, attendance_score, homework_score, midterm_score, final_score, total_score, grade_level, credit_obtained, course_gpa, retake_flag, retake_course_flag, status) VALUES
  (2, 16, 1, 95, 98, 90, 88, 92, 92.0, '优', 1, 4.0, 0, 0, '已归档'),
  (2, 28, 2, 85, 80, 88, 0, 0, 84.0, '良', 1, 3.0, 0, 0, '已归档');
-- 学生3（2023级 数学）历史成绩
INSERT INTO score (student_id, course_id, semester_id, regular_score, attendance_score, homework_score, midterm_score, final_score, total_score, grade_level, credit_obtained, course_gpa, retake_flag, retake_course_flag, status) VALUES
  (3, 15, 1, 75, 80, 70, 68, 72, 72.0, '中', 1, 2.0, 0, 0, '已归档'),
  (3, 18, 2, 55, 60, 50, 45, 52, 52.0, '不及格', 0, 0.0, 0, 1, '已归档'); -- 离散数学挂科重修

-- ---------- 23. 培养方案 training_plan（计算机科学与技术 2023级 当前学期） ----------
INSERT INTO training_plan (major, grade, semester_id, required_courses, elective_courses, min_credits, max_credits, grad_total_credits, grad_required_credits, grad_elective_credits, gen_edu_credits, remark) VALUES
  ('计算机科学与技术', '2023级', 3, '1,2,3,4,16', '5,6,7,8,9,10,11,12,13,14,15,17,18,19', 15.0, 30.0, 120.0, 80.0, 30.0, 10.0, '2023级计算机科学与技术培养方案（当前学期）');

-- ---------- 24. 公告 announcement（教务发布） ----------
INSERT INTO announcement (title, content, publisher_id, is_pinned, status, published_at) VALUES
  ('2026-2027 学年第一学期选课通知', '本学期正式选课将于 9 月 25 日 09:00 开放，9 月 28 日 23:59 截止；补退选阶段为 10 月 8 日至 10 月 12 日。请同学们提前核对培养方案与个人培养计划，逾期不再受理。', 42, 1, 'PUBLISHED', '2026-09-20 08:00:00'),
  ('关于成绩录入与审核的说明', '本学期成绩录入截止时间为 2027 年 1 月 20 日。教师录入后须经教务审核方可生效，审核通过的成绩将自动结算学分与绩点并入档。', 42, 0, 'PUBLISHED', '2026-09-20 08:30:00');

-- ---------- 25. 操作日志 operation_log（审计示例） ----------
INSERT INTO operation_log (user_id, username, action, target, detail, ip, created_at) VALUES
  (42, 'admin', '学期创建', '2026-2027-1', '创建当前学期并设置选课时间窗口', '127.0.0.1', '2026-09-01 09:00:00'),
  (42, 'admin', '学期归档', '2025-2026-2', '归档历史学期成绩、学分、课表数据', '127.0.0.1', '2026-08-30 17:00:00'),
  (42, 'admin', '成绩审核', 'score', '审核通过学生成绩共 8 条', '127.0.0.1', '2026-07-10 15:30:00');

COMMIT;

-- =====================================================================
-- 数据量合计：
--   sys_user   42 行（学生 3 / 教师 38 / 教务 1）
--   student    3 行；teacher 38 行；course 40 行
--   course_schedule  50 行；course_prerequisite 9 行
--   student_course  5 行；wishlist 3 行（示例）；seat_subscription 1 行（示例）
--   message 11 行；user_preference 42 行；selection_rule 1 行
--   selection_period 3 行；anomaly_ticket 3 行；selection_ticket 1 行 + item 1 行
-- =====================================================================

