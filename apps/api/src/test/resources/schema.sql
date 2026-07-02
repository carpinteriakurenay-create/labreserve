DROP TABLE IF EXISTS reviews;
DROP TABLE IF EXISTS repair_logs;
DROP TABLE IF EXISTS students;
DROP TABLE IF EXISTS messages;
DROP TABLE IF EXISTS borrows;
DROP TABLE IF EXISTS courses;
DROP TABLE IF EXISTS notices;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS equipment;
DROP TABLE IF EXISTS lab_hours;
DROP TABLE IF EXISTS lab_categories;
DROP TABLE IF EXISTS labs;
DROP TABLE IF EXISTS users;

CREATE TABLE users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    real_name VARCHAR(50) NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    role VARCHAR(20) NOT NULL DEFAULT 'STUDENT',
    avatar VARCHAR(255),
    enabled INT NOT NULL DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_users_username ON users(username);
CREATE INDEX idx_users_role ON users(role);
CREATE INDEX idx_users_enabled ON users(enabled);
CREATE INDEX idx_users_deleted ON users(deleted);

CREATE TABLE labs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    location VARCHAR(200),
    capacity INT,
    description VARCHAR(500),
    image_url VARCHAR(255),
    equipment_num INT DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    manager_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_labs_status ON labs(status);
CREATE INDEX idx_labs_name ON labs(name);
CREATE INDEX idx_labs_deleted ON labs(deleted);

CREATE TABLE lab_categories (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(50) NOT NULL,
    description VARCHAR(200),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_lab_categories_name ON lab_categories(name);
CREATE INDEX idx_lab_categories_deleted ON lab_categories(deleted);

CREATE TABLE lab_hours (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lab_id BIGINT NOT NULL,
    day_of_week INT NOT NULL,
    open_time VARCHAR(5) NOT NULL,
    close_time VARCHAR(5) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_lab_hours_day_open ON lab_hours(lab_id, day_of_week, open_time);
CREATE INDEX idx_lab_hours_lab ON lab_hours(lab_id);
CREATE INDEX idx_lab_hours_deleted ON lab_hours(deleted);

CREATE TABLE equipment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lab_id BIGINT NOT NULL,
    name VARCHAR(100) NOT NULL,
    model VARCHAR(100),
    serial_number VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_equipment_serial ON equipment(serial_number);
CREATE INDEX idx_equipment_lab ON equipment(lab_id);
CREATE INDEX idx_equipment_status ON equipment(status);
CREATE INDEX idx_equipment_name ON equipment(name);
CREATE INDEX idx_equipment_deleted ON equipment(deleted);

CREATE TABLE bookings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lab_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    date DATE NOT NULL,
    start_time VARCHAR(5) NOT NULL,
    end_time VARCHAR(5) NOT NULL,
    purpose VARCHAR(500),
    person_count INT DEFAULT 1,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(500),
    approver_id BIGINT,
    approved_at TIMESTAMP,
    completed_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_bookings_lab_date ON bookings(lab_id, date);
CREATE INDEX idx_bookings_user ON bookings(user_id);
CREATE INDEX idx_bookings_status ON bookings(status);
CREATE INDEX idx_bookings_approver ON bookings(approver_id);
CREATE INDEX idx_bookings_deleted ON bookings(deleted);

CREATE TABLE borrows (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipment_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    borrow_date DATE NOT NULL,
    expected_return DATE NOT NULL,
    actual_return DATE,
    purpose VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reject_reason VARCHAR(500),
    approver_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_borrows_equipment ON borrows(equipment_id);
CREATE INDEX idx_borrows_user ON borrows(user_id);
CREATE INDEX idx_borrows_status ON borrows(status);
CREATE INDEX idx_borrows_approver ON borrows(approver_id);
CREATE INDEX idx_borrows_deleted ON borrows(deleted);

CREATE TABLE courses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    lab_id BIGINT NOT NULL,
    teacher_id BIGINT NOT NULL,
    semester VARCHAR(20) NOT NULL,
    day_of_week INT NOT NULL,
    start_time VARCHAR(5) NOT NULL,
    end_time VARCHAR(5) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    class_name VARCHAR(50),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_courses_lab_day ON courses(lab_id, day_of_week);
CREATE INDEX idx_courses_teacher ON courses(teacher_id);
CREATE INDEX idx_courses_semester ON courses(semester);
CREATE INDEX idx_courses_deleted ON courses(deleted);

CREATE TABLE notices (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content TEXT NOT NULL,
    type VARCHAR(20) NOT NULL DEFAULT 'GENERAL',
    priority VARCHAR(20) NOT NULL DEFAULT 'NORMAL',
    publisher_id BIGINT NOT NULL,
    lab_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_notices_type ON notices(type);
CREATE INDEX idx_notices_priority ON notices(priority);
CREATE INDEX idx_notices_created ON notices(created_at);
CREATE INDEX idx_notices_deleted ON notices(deleted);

CREATE TABLE reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    booking_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    lab_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment VARCHAR(1000),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);
CREATE UNIQUE INDEX uk_reviews_booking ON reviews(booking_id);
CREATE INDEX idx_reviews_lab ON reviews(lab_id);
CREATE INDEX idx_reviews_user ON reviews(user_id);
CREATE INDEX idx_reviews_deleted ON reviews(deleted);

CREATE TABLE repair_logs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    equipment_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    description VARCHAR(500) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_repair_logs_equipment ON repair_logs(equipment_id);
CREATE INDEX idx_repair_logs_status ON repair_logs(status);
CREATE INDEX idx_repair_logs_deleted ON repair_logs(deleted);

CREATE TABLE students (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    lab_id BIGINT,
    name VARCHAR(50) NOT NULL,
    gender VARCHAR(10),
    age INT,
    address VARCHAR(200),
    creator_id BIGINT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_students_lab ON students(lab_id);
CREATE INDEX idx_students_name ON students(name);
CREATE INDEX idx_students_deleted ON students(deleted);

CREATE TABLE messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    sender_id BIGINT NOT NULL,
    receiver_id BIGINT NOT NULL,
    title VARCHAR(200),
    content TEXT,
    is_read INT DEFAULT 0,
    read_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    deleted INT NOT NULL DEFAULT 0
);
CREATE INDEX idx_messages_receiver_read ON messages(receiver_id, is_read);
CREATE INDEX idx_messages_sender ON messages(sender_id);
CREATE INDEX idx_messages_deleted ON messages(deleted);
