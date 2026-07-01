package com.labreserve.support;

import com.labreserve.entity.*;
import com.labreserve.enums.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TestDataFactory {

    public static User createUser(Long id, String username, String realName, UserRole role) {
        User user = new User();
        user.setId(id);
        user.setUsername(username);
        user.setRealName(realName);
        user.setPassword("$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy");
        user.setRole(role);
        user.setEnabled(1);
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        user.setDeleted(0);
        return user;
    }

    public static Lab createLab(Long id, String name, LabStatus status) {
        Lab lab = new Lab();
        lab.setId(id);
        lab.setName(name);
        lab.setLocation("Test Location");
        lab.setCapacity(40);
        lab.setDescription("Test lab");
        lab.setStatus(status);
        lab.setManagerId(3L);
        lab.setCreatedAt(LocalDateTime.now());
        lab.setUpdatedAt(LocalDateTime.now());
        lab.setDeleted(0);
        return lab;
    }

    public static LabHours createLabHours(Long id, Long labId, int dayOfWeek, String openTime, String closeTime) {
        LabHours hours = new LabHours();
        hours.setId(id);
        hours.setLabId(labId);
        hours.setDayOfWeek(dayOfWeek);
        hours.setOpenTime(openTime);
        hours.setCloseTime(closeTime);
        hours.setCreatedAt(LocalDateTime.now());
        hours.setUpdatedAt(LocalDateTime.now());
        hours.setDeleted(0);
        return hours;
    }

    public static Booking createBooking(Long id, Long labId, Long userId, BookingStatus status,
                                         LocalDate date, String startTime, String endTime) {
        Booking booking = new Booking();
        booking.setId(id);
        booking.setLabId(labId);
        booking.setUserId(userId);
        booking.setDate(date);
        booking.setStartTime(startTime);
        booking.setEndTime(endTime);
        booking.setPurpose("Test purpose");
        booking.setPersonCount(1);
        booking.setStatus(status);
        booking.setCreatedAt(LocalDateTime.now());
        booking.setUpdatedAt(LocalDateTime.now());
        booking.setDeleted(0);
        return booking;
    }

    public static Equipment createEquipment(Long id, Long labId, String name, String serialNumber, EquipmentStatus status) {
        Equipment equipment = new Equipment();
        equipment.setId(id);
        equipment.setLabId(labId);
        equipment.setName(name);
        equipment.setModel("Test Model");
        equipment.setSerialNumber(serialNumber);
        equipment.setDescription("Test equipment");
        equipment.setStatus(status);
        equipment.setCreatedAt(LocalDateTime.now());
        equipment.setUpdatedAt(LocalDateTime.now());
        equipment.setDeleted(0);
        return equipment;
    }

    public static Borrow createBorrow(Long id, Long equipmentId, Long userId, BorrowStatus status,
                                       LocalDate borrowDate, LocalDate expectedReturn) {
        Borrow borrow = new Borrow();
        borrow.setId(id);
        borrow.setEquipmentId(equipmentId);
        borrow.setUserId(userId);
        borrow.setBorrowDate(borrowDate);
        borrow.setExpectedReturn(expectedReturn);
        borrow.setPurpose("Test borrow");
        borrow.setStatus(status);
        borrow.setCreatedAt(LocalDateTime.now());
        borrow.setUpdatedAt(LocalDateTime.now());
        borrow.setDeleted(0);
        return borrow;
    }

    public static Notice createNotice(Long id, String title, String content, NoticeType type,
                                       NoticePriority priority, Long publisherId) {
        Notice notice = new Notice();
        notice.setId(id);
        notice.setTitle(title);
        notice.setContent(content);
        notice.setType(type);
        notice.setPriority(priority);
        notice.setPublisherId(publisherId);
        notice.setCreatedAt(LocalDateTime.now());
        notice.setUpdatedAt(LocalDateTime.now());
        notice.setDeleted(0);
        return notice;
    }

    public static Course createCourse(Long id, Long labId, Long teacherId, String name, String semester,
                                       int dayOfWeek, String startTime, String endTime) {
        Course course = new Course();
        course.setId(id);
        course.setName(name);
        course.setLabId(labId);
        course.setTeacherId(teacherId);
        course.setSemester(semester);
        course.setDayOfWeek(dayOfWeek);
        course.setStartTime(startTime);
        course.setEndTime(endTime);
        course.setStartDate(LocalDate.now().minusDays(7));
        course.setEndDate(LocalDate.now().plusDays(60));
        course.setClassName("CS2101");
        course.setCreatedAt(LocalDateTime.now());
        course.setUpdatedAt(LocalDateTime.now());
        course.setDeleted(0);
        return course;
    }

    public static Review createReview(Long id, Long bookingId, Long userId, Long labId, int rating, String comment) {
        Review review = new Review();
        review.setId(id);
        review.setBookingId(bookingId);
        review.setUserId(userId);
        review.setLabId(labId);
        review.setRating(rating);
        review.setComment(comment);
        review.setCreatedAt(LocalDateTime.now());
        review.setUpdatedAt(LocalDateTime.now());
        review.setDeleted(0);
        return review;
    }

    public static RepairLog createRepairLog(Long id, Long equipmentId, Long reporterId,
                                             String description, RepairStatus status) {
        RepairLog log = new RepairLog();
        log.setId(id);
        log.setEquipmentId(equipmentId);
        log.setReporterId(reporterId);
        log.setDescription(description);
        log.setStatus(status);
        log.setCreatedAt(LocalDateTime.now());
        log.setUpdatedAt(LocalDateTime.now());
        log.setDeleted(0);
        return log;
    }

    public static Student createStudent(Long id, Long labId, String name, String gender, int age, String address) {
        Student student = new Student();
        student.setId(id);
        student.setLabId(labId);
        student.setName(name);
        student.setGender(gender);
        student.setAge(age);
        student.setAddress(address);
        student.setCreatorId(3L);
        student.setCreatedAt(LocalDateTime.now());
        student.setUpdatedAt(LocalDateTime.now());
        student.setDeleted(0);
        return student;
    }
}
