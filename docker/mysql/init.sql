-- ============================================================================
-- LabReserve 数据库初始化脚本
-- 版本：1.0 | 日期：2026-06-29
-- ============================================================================

CREATE DATABASE IF NOT EXISTS labreserve_dev
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

GRANT ALL PRIVILEGES ON labreserve_dev.* TO 'labreserve'@'%';
FLUSH PRIVILEGES;

USE labreserve_dev;

-- ============================================================================
-- 1. users — 用户表（学生/教师/管理员）
-- ============================================================================
CREATE TABLE IF NOT EXISTS users (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  username    VARCHAR(50)   NOT NULL,
  real_name   VARCHAR(50)   NOT NULL,
  password    VARCHAR(255)  NOT NULL,
  email       VARCHAR(100)  DEFAULT NULL,
  phone       VARCHAR(20)   DEFAULT NULL,
  role        VARCHAR(16)   NOT NULL DEFAULT 'STUDENT',
  avatar      VARCHAR(255)  DEFAULT NULL,
  enabled     TINYINT       NOT NULL DEFAULT 1,
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY  uk_users_username (username),
  KEY         idx_users_role (role),
  KEY         idx_users_enabled (enabled),
  KEY         idx_users_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 2. labs — 实验室信息表
-- ============================================================================
CREATE TABLE IF NOT EXISTS labs (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  name          VARCHAR(100)  NOT NULL,
  location      VARCHAR(200)  DEFAULT NULL,
  capacity      INT           NOT NULL DEFAULT 0,
  description   TEXT          DEFAULT NULL,
  image_url     VARCHAR(255)  DEFAULT NULL,
  equipment_num INT           NOT NULL DEFAULT 0,
  status        VARCHAR(16)   NOT NULL DEFAULT 'AVAILABLE',
  manager_id    BIGINT        DEFAULT NULL,
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY           idx_labs_status (status),
  KEY           idx_labs_name (name),
  KEY           idx_labs_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 3. lab_hours — 实验室每周开放时段
-- ============================================================================
CREATE TABLE IF NOT EXISTS lab_hours (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  lab_id      BIGINT        NOT NULL,
  day_of_week TINYINT       NOT NULL,
  open_time   VARCHAR(10)   NOT NULL,
  close_time  VARCHAR(10)   NOT NULL,
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY  uk_lab_hours_day_open (lab_id, day_of_week, open_time),
  KEY         idx_lab_hours_lab (lab_id),
  KEY         idx_lab_hours_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 4. lab_categories — 实验室分类
-- ============================================================================
CREATE TABLE IF NOT EXISTS lab_categories (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  name        VARCHAR(100)  NOT NULL,
  description TEXT          DEFAULT NULL,
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY  uk_lab_categories_name (name),
  KEY         idx_lab_categories_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 5. equipment — 设备资产表
-- ============================================================================
CREATE TABLE IF NOT EXISTS equipment (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  lab_id        BIGINT        NOT NULL,
  name          VARCHAR(100)  NOT NULL,
  model         VARCHAR(100)  DEFAULT NULL,
  serial_number VARCHAR(100)  NOT NULL,
  description   TEXT          DEFAULT NULL,
  status        VARCHAR(16)   NOT NULL DEFAULT 'AVAILABLE',
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY  uk_equipment_serial (serial_number),
  KEY         idx_equipment_lab (lab_id),
  KEY         idx_equipment_status (status),
  KEY         idx_equipment_name (name),
  KEY         idx_equipment_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 6. bookings — 预约记录表（核心）
-- ============================================================================
CREATE TABLE IF NOT EXISTS bookings (
  id            BIGINT        NOT NULL AUTO_INCREMENT,
  lab_id        BIGINT        NOT NULL,
  user_id       BIGINT        NOT NULL,
  date          DATE          NOT NULL,
  start_time    VARCHAR(10)   NOT NULL,
  end_time      VARCHAR(10)   NOT NULL,
  purpose       TEXT          DEFAULT NULL,
  person_count  INT           NOT NULL DEFAULT 1,
  status        VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
  reject_reason TEXT          DEFAULT NULL,
  approver_id   BIGINT        DEFAULT NULL,
  approved_at   DATETIME      DEFAULT NULL,
  completed_at  DATETIME      DEFAULT NULL,
  created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted       TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY         idx_bookings_lab_date (lab_id, date),
  KEY         idx_bookings_user (user_id),
  KEY         idx_bookings_status (status),
  KEY         idx_bookings_approver (approver_id),
  KEY         idx_bookings_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 7. borrows — 设备借用记录表
-- ============================================================================
CREATE TABLE IF NOT EXISTS borrows (
  id              BIGINT        NOT NULL AUTO_INCREMENT,
  equipment_id    BIGINT        NOT NULL,
  user_id         BIGINT        NOT NULL,
  borrow_date     DATE          NOT NULL,
  expected_return DATE          NOT NULL,
  actual_return   DATE          DEFAULT NULL,
  purpose         TEXT          DEFAULT NULL,
  status          VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
  reject_reason   TEXT          DEFAULT NULL,
  approver_id     BIGINT        DEFAULT NULL,
  created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted         TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY         idx_borrows_equipment (equipment_id),
  KEY         idx_borrows_user (user_id),
  KEY         idx_borrows_status (status),
  KEY         idx_borrows_approver (approver_id),
  KEY         idx_borrows_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 8. courses — 课程安排表
-- ============================================================================
CREATE TABLE IF NOT EXISTS courses (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  name        VARCHAR(100)  NOT NULL,
  lab_id      BIGINT        NOT NULL,
  teacher_id  BIGINT        NOT NULL,
  semester    VARCHAR(20)   NOT NULL,
  day_of_week TINYINT       NOT NULL,
  start_time  VARCHAR(10)   NOT NULL,
  end_time    VARCHAR(10)   NOT NULL,
  start_date  DATE          NOT NULL,
  end_date    DATE          NOT NULL,
  class_name  VARCHAR(100)  DEFAULT NULL,
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY         idx_courses_lab_day (lab_id, day_of_week),
  KEY         idx_courses_teacher (teacher_id),
  KEY         idx_courses_semester (semester),
  KEY         idx_courses_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 9. reviews — 实验室评价表
-- ============================================================================
CREATE TABLE IF NOT EXISTS reviews (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  booking_id  BIGINT        NOT NULL,
  user_id     BIGINT        NOT NULL,
  lab_id      BIGINT        NOT NULL,
  rating      TINYINT       NOT NULL,
  comment     TEXT          DEFAULT NULL,
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY  uk_reviews_booking (booking_id),
  KEY         idx_reviews_lab (lab_id),
  KEY         idx_reviews_user (user_id),
  KEY         idx_reviews_deleted (deleted),
  CONSTRAINT  chk_reviews_rating CHECK (rating BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 10. notices — 通知公告表
-- ============================================================================
CREATE TABLE IF NOT EXISTS notices (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  title        VARCHAR(200)  NOT NULL,
  content      TEXT          NOT NULL,
  type         VARCHAR(16)   NOT NULL DEFAULT 'GENERAL',
  priority     VARCHAR(16)   NOT NULL DEFAULT 'NORMAL',
  publisher_id BIGINT        NOT NULL,
  lab_id       BIGINT        DEFAULT NULL,
  created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY         idx_notices_type (type),
  KEY         idx_notices_priority (priority),
  KEY         idx_notices_created (created_at),
  KEY         idx_notices_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 11. students — 学生/人员信息表（管理员录入）
-- ============================================================================
CREATE TABLE IF NOT EXISTS students (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  lab_id      BIGINT        NOT NULL,
  name        VARCHAR(50)   NOT NULL,
  gender      VARCHAR(10)   DEFAULT NULL,
  age         INT           DEFAULT NULL,
  address     TEXT          DEFAULT NULL,
  creator_id  BIGINT        NOT NULL,
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY         idx_students_lab (lab_id),
  KEY         idx_students_name (name),
  KEY         idx_students_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 12. messages — 站内消息表
-- ============================================================================
CREATE TABLE IF NOT EXISTS messages (
  id          BIGINT        NOT NULL AUTO_INCREMENT,
  sender_id   BIGINT        NOT NULL,
  receiver_id BIGINT        NOT NULL,
  title       VARCHAR(200)  DEFAULT NULL,
  content     TEXT          NOT NULL,
  is_read     TINYINT       NOT NULL DEFAULT 0,
  read_at     DATETIME      DEFAULT NULL,
  created_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted     TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY         idx_messages_receiver_read (receiver_id, is_read),
  KEY         idx_messages_sender (sender_id),
  KEY         idx_messages_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 13. repair_logs — 设备报修记录表
-- ============================================================================
CREATE TABLE IF NOT EXISTS repair_logs (
  id           BIGINT        NOT NULL AUTO_INCREMENT,
  equipment_id BIGINT        NOT NULL,
  reporter_id  BIGINT        NOT NULL,
  description  TEXT          NOT NULL,
  status       VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
  created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  deleted      TINYINT       NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  KEY         idx_repair_logs_equipment (equipment_id),
  KEY         idx_repair_logs_status (status),
  KEY         idx_repair_logs_deleted (deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================================
-- 种子数据
-- ============================================================================

SET NAMES utf8mb4;

-- 种子用户（BCrypt 加密密码）
-- 密码: password123 -> $2b$10$gmiBQqb873lHaAHvTlxt6uymWuCIWOyk4QN9Nrk8yumfMY2TS/cNm
-- 密码: admin123   -> $2b$10$cCla6HDGcbFxx7YPHfmqUeIUtcY6nuCEvQh12nK.npiADZPmPAWrm

INSERT INTO users (username, real_name, password, email, phone, role) VALUES
  ('2021001', 'Zhang San', '$2b$10$gmiBQqb873lHaAHvTlxt6uymWuCIWOyk4QN9Nrk8yumfMY2TS/cNm', 'zhangsan@univ.edu.cn', '13800001111', 'STUDENT'),
  ('T001', 'Prof. Li', '$2b$10$gmiBQqb873lHaAHvTlxt6uymWuCIWOyk4QN9Nrk8yumfMY2TS/cNm', 'lijs@univ.edu.cn', '13900002222', 'TEACHER'),
  ('admin', 'Admin Wang', '$2b$10$cCla6HDGcbFxx7YPHfmqUeIUtcY6nuCEvQh12nK.npiADZPmPAWrm', 'admin@univ.edu.cn', '13700000000', 'ADMIN');

-- 种子实验室
INSERT INTO labs (name, location, capacity, description, status, manager_id) VALUES
  ('计算机组成原理实验室', '教学楼A-301', 40, '配备 40 台 FPGA 开发板，用于计算机组成原理课程实验', 'AVAILABLE', 3),
  ('计算机网络实验室',     '教学楼B-201', 48, '配备 Cisco 网络设备，用于计算机网络课程实验',       'AVAILABLE', 3);
