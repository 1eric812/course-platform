-- =====================================================================
-- 高校选课平台 · 数据库创建脚本（00-create-database.sql）
-- 适用：MySQL 8.0+
-- 执行：mysql -u root -p < 00-create-database.sql
-- 说明：如需独立数据库账号，取消下方注释并按需修改密码
-- =====================================================================

CREATE DATABASE IF NOT EXISTS course_platform
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_general_ci;

-- 可选：为应用创建专用账号（避免直接用 root）
-- CREATE USER IF NOT EXISTS 'course_app'@'localhost' IDENTIFIED BY 'CHANGE_ME_APP_PASSWORD';
-- GRANT ALL PRIVILEGES ON course_platform.* TO 'course_app'@'localhost';
-- FLUSH PRIVILEGES;

-- 建库后请依次执行：
--   1) mysql -u root -p course_platform < schema.sql
--   2) mysql -u root -p course_platform < data.sql
