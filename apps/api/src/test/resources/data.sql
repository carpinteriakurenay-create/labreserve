-- Users: id=1 student, id=2 teacher, id=3 admin
-- Passwords are BCrypt hash of "password123"
INSERT INTO users (id, username, real_name, password, email, phone, role, enabled) VALUES
(1, 'student1', 'Student One', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'student1@test.com', '13800000001', 'STUDENT', 1),
(2, 'teacher1', 'Teacher One', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'teacher1@test.com', '13800000002', 'TEACHER', 1),
(3, 'admin1', 'Admin One', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin1@test.com', '13800000003', 'ADMIN', 1);

-- Labs
INSERT INTO labs (id, name, location, capacity, description, status, manager_id) VALUES
(1, 'Computer Lab A', 'Building A-301', 40, 'FPGA development lab', 'AVAILABLE', 3),
(2, 'Physics Lab B', 'Building B-201', 30, 'Physics experiment lab', 'MAINTENANCE', 3);

-- Lab hours for lab 1: Mon-Fri 08:00-18:00
INSERT INTO lab_hours (lab_id, day_of_week, open_time, close_time) VALUES
(1, 1, '08:00', '18:00'),
(1, 2, '08:00', '18:00'),
(1, 3, '08:00', '18:00'),
(1, 4, '08:00', '18:00'),
(1, 5, '08:00', '18:00');

-- Lab hours for lab 2: Mon/Wed/Fri 09:00-17:00
INSERT INTO lab_hours (lab_id, day_of_week, open_time, close_time) VALUES
(2, 1, '09:00', '17:00'),
(2, 3, '09:00', '17:00'),
(2, 5, '09:00', '17:00');

-- Bookings
INSERT INTO bookings (id, lab_id, user_id, date, start_time, end_time, purpose, person_count, status) VALUES
(1, 1, 1, CURRENT_DATE, '10:00', '12:00', 'FPGA experiment', 2, 'APPROVED'),
(2, 1, 1, CURRENT_DATE, '14:00', '16:00', 'Course design', 3, 'PENDING'),
(3, 1, 1, CURRENT_DATE, '08:00', '09:00', 'Quick test', 1, 'COMPLETED');

-- Equipment
INSERT INTO equipment (id, lab_id, name, model, serial_number, status) VALUES
(1, 1, 'FPGA Board X1', 'Xilinx Artix-7', 'FPGA-2026-001', 'AVAILABLE'),
(2, 1, 'Oscilloscope D1', 'Tektronix TBS1000', 'OSC-2026-001', 'MAINTENANCE');

-- Course
INSERT INTO courses (id, name, lab_id, teacher_id, semester, day_of_week, start_time, end_time, start_date, end_date, class_name) VALUES
(1, 'Digital Logic Design', 1, 2, '2025-2026-2', 3, '08:00', '10:00', DATEADD('DAY', -7, CURRENT_DATE), DATEADD('DAY', 60, CURRENT_DATE), 'CS2101');
