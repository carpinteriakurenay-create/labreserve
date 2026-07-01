package com.labreserve.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labreserve.dto.ApprovalRequest;
import com.labreserve.dto.BookingCreateRequest;
import com.labreserve.dto.BookingUpdateRequest;
import com.labreserve.dto.BookingVO;
import com.labreserve.dto.TimeSlot;
import com.labreserve.entity.Booking;
import com.labreserve.entity.Course;
import com.labreserve.entity.Lab;
import com.labreserve.entity.LabHours;
import com.labreserve.entity.User;
import com.labreserve.enums.BookingStatus;
import com.labreserve.exception.BusinessException;
import com.labreserve.mapper.BookingMapper;
import com.labreserve.mapper.CourseMapper;
import com.labreserve.mapper.LabHoursMapper;
import com.labreserve.mapper.LabMapper;
import com.labreserve.mapper.UserMapper;
import com.labreserve.util.TimeConflictResolver;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private final BookingMapper bookingMapper;
    private final LabMapper labMapper;
    private final LabHoursMapper labHoursMapper;
    private final CourseMapper courseMapper;
    private final UserMapper userMapper;
    private final RedissonClient redissonClient;

    public BookingService(BookingMapper bookingMapper, LabMapper labMapper,
                          LabHoursMapper labHoursMapper, CourseMapper courseMapper,
                          UserMapper userMapper, RedissonClient redissonClient) {
        this.bookingMapper = bookingMapper;
        this.labMapper = labMapper;
        this.labHoursMapper = labHoursMapper;
        this.courseMapper = courseMapper;
        this.userMapper = userMapper;
        this.redissonClient = redissonClient;
    }

    @Transactional
    public BookingVO createBooking(BookingCreateRequest request, Long userId) {
        LocalDate date = LocalDate.parse(request.getDate());

        Lab lab = labMapper.selectById(request.getLabId());
        if (lab == null) {
            throw new BusinessException("NOT_FOUND", "实验室不存在");
        }

        List<LabHours> openHours = labHoursMapper.selectList(
                new LambdaQueryWrapper<LabHours>().eq(LabHours::getLabId, request.getLabId()));

        List<Booking> conflictingBookings = bookingMapper.selectList(
                new LambdaQueryWrapper<Booking>()
                        .eq(Booking::getLabId, request.getLabId())
                        .eq(Booking::getDate, date)
                        .and(w -> w.eq(Booking::getStatus, BookingStatus.PENDING)
                                .or()
                                .eq(Booking::getStatus, BookingStatus.APPROVED)));

        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getLabId, request.getLabId())
                        .le(Course::getStartDate, date)
                        .ge(Course::getEndDate, date));

        List<String> conflicts = TimeConflictResolver.checkConflicts(
                request, lab, openHours, conflictingBookings, courses);

        if (!conflicts.isEmpty()) {
            String code = conflicts.get(0);
            String message = switch (code) {
                case "LAB_CLOSED" -> "实验室当前未开放";
                case "OUTSIDE_OPEN_HOURS" -> "预约时间不在实验室开放时段内";
                case "TIME_CONFLICT" -> "预约时间与已有安排冲突";
                default -> "预约冲突";
            };
            throw new BusinessException(code, message);
        }

        Booking booking = new Booking();
        booking.setLabId(request.getLabId());
        booking.setUserId(userId);
        booking.setDate(date);
        booking.setStartTime(request.getStartTime());
        booking.setEndTime(request.getEndTime());
        booking.setPurpose(request.getPurpose());
        booking.setPersonCount(request.getPersonCount() != null ? request.getPersonCount() : 1);
        booking.setStatus(BookingStatus.PENDING);

        bookingMapper.insert(booking);
        return toBookingVO(booking);
    }

    public BookingVO getBookingById(Long id) {
        Booking booking = bookingMapper.selectById(id);
        if (booking == null) {
            throw new BusinessException("NOT_FOUND", "预约不存在");
        }
        BookingVO vo = toBookingVO(booking);
        populateJoinedFields(Collections.singletonList(vo));
        return vo;
    }

    public IPage<BookingVO> listBookings(String statusStr, Long labId, Long userId,
                                          String dateFrom, String dateTo,
                                          int pageNum, int pageSize) {
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();

        if (statusStr != null && !statusStr.isBlank()) {
            try {
                wrapper.eq(Booking::getStatus, BookingStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("INVALID_STATUS", "无效的预约状态");
            }
        }
        if (labId != null) {
            wrapper.eq(Booking::getLabId, labId);
        }
        if (userId != null) {
            wrapper.eq(Booking::getUserId, userId);
        }
        if (dateFrom != null && !dateFrom.isBlank()) {
            wrapper.ge(Booking::getDate, LocalDate.parse(dateFrom));
        }
        if (dateTo != null && !dateTo.isBlank()) {
            wrapper.le(Booking::getDate, LocalDate.parse(dateTo));
        }

        wrapper.orderByDesc(Booking::getCreatedAt);

        Page<Booking> page = new Page<>(pageNum, pageSize);
        IPage<Booking> result = bookingMapper.selectPage(page, wrapper);
        IPage<BookingVO> voPage = result.convert(this::toBookingVO);
        populateJoinedFields(voPage.getRecords());
        return voPage;
    }

    public IPage<BookingVO> listMyBookings(Long userId, String statusStr, int pageNum, int pageSize) {
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Booking::getUserId, userId);

        if (statusStr != null && !statusStr.isBlank()) {
            try {
                wrapper.eq(Booking::getStatus, BookingStatus.valueOf(statusStr));
            } catch (IllegalArgumentException e) {
                throw new BusinessException("INVALID_STATUS", "无效的预约状态");
            }
        }

        wrapper.orderByDesc(Booking::getCreatedAt);

        Page<Booking> page = new Page<>(pageNum, pageSize);
        IPage<Booking> result = bookingMapper.selectPage(page, wrapper);
        IPage<BookingVO> voPage = result.convert(this::toBookingVO);
        populateJoinedFields(voPage.getRecords());
        return voPage;
    }

    public IPage<BookingVO> listPendingApprovals(int pageNum, int pageSize) {
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Booking::getStatus, BookingStatus.PENDING)
                .orderByAsc(Booking::getCreatedAt);

        Page<Booking> page = new Page<>(pageNum, pageSize);
        IPage<Booking> result = bookingMapper.selectPage(page, wrapper);
        IPage<BookingVO> voPage = result.convert(this::toBookingVO);
        populateJoinedFields(voPage.getRecords());
        return voPage;
    }

    @Transactional
    public BookingVO updateBooking(Long id, BookingUpdateRequest request, Long userId) {
        Booking booking = bookingMapper.selectById(id);
        if (booking == null) {
            throw new BusinessException("NOT_FOUND", "预约不存在");
        }
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("FORBIDDEN", "只能修改自己的预约");
        }
        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new BusinessException("BOOKING_NOT_EDITABLE", "只有待审批状态的预约可以修改");
        }

        LambdaUpdateWrapper<Booking> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Booking::getId, id);

        if (request.getDate() != null) {
            wrapper.set(Booking::getDate, LocalDate.parse(request.getDate()));
        }
        if (request.getStartTime() != null) {
            wrapper.set(Booking::getStartTime, request.getStartTime());
        }
        if (request.getEndTime() != null) {
            wrapper.set(Booking::getEndTime, request.getEndTime());
        }
        if (request.getPurpose() != null) {
            wrapper.set(Booking::getPurpose, request.getPurpose());
        }
        if (request.getPersonCount() != null) {
            wrapper.set(Booking::getPersonCount, request.getPersonCount());
        }

        bookingMapper.update(wrapper);

        Booking updated = bookingMapper.selectById(id);
        return toBookingVO(updated);
    }

    @Transactional
    public BookingVO approveBooking(Long id, boolean approved, String rejectReason, Long approverId) {
        String lockKey = "booking:lock:" + id;
        RLock lock = redissonClient.getLock(lockKey);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException("LOCK_FAILED", "系统繁忙，请稍后重试");
        }
        if (!locked) {
            throw new BusinessException("LOCK_FAILED", "该预约正在被其他管理员处理，请稍后重试");
        }
        try {
            Booking booking = bookingMapper.selectById(id);
            if (booking == null) {
                throw new BusinessException("NOT_FOUND", "预约不存在");
            }
            if (booking.getStatus() != BookingStatus.PENDING) {
                throw new BusinessException("ALREADY_PROCESSED", "该预约已被处理");
            }

            LambdaUpdateWrapper<Booking> wrapper = new LambdaUpdateWrapper<>();
            wrapper.eq(Booking::getId, id);

            if (approved) {
                Lab lab = labMapper.selectById(booking.getLabId());
                List<LabHours> openHours = labHoursMapper.selectList(
                        new LambdaQueryWrapper<LabHours>().eq(LabHours::getLabId, booking.getLabId()));

                List<Booking> conflictingBookings = bookingMapper.selectList(
                        new LambdaQueryWrapper<Booking>()
                                .eq(Booking::getLabId, booking.getLabId())
                                .eq(Booking::getDate, booking.getDate())
                                .in(Booking::getStatus, BookingStatus.PENDING, BookingStatus.APPROVED)
                                .ne(Booking::getId, id));

                List<Course> courses = courseMapper.selectList(
                        new LambdaQueryWrapper<Course>()
                                .eq(Course::getLabId, booking.getLabId())
                                .le(Course::getStartDate, booking.getDate())
                                .ge(Course::getEndDate, booking.getDate()));

                BookingCreateRequest recheckRequest = new BookingCreateRequest();
                recheckRequest.setLabId(booking.getLabId());
                recheckRequest.setDate(booking.getDate().toString());
                recheckRequest.setStartTime(booking.getStartTime());
                recheckRequest.setEndTime(booking.getEndTime());

                List<String> conflicts = TimeConflictResolver.checkConflicts(
                        recheckRequest, lab, openHours, conflictingBookings, courses);

                if (!conflicts.isEmpty()) {
                    throw new BusinessException(conflicts.get(0),
                            conflicts.get(0).equals("TIME_CONFLICT") ? "该时段已被占用" : "审批失败");
                }

                wrapper.set(Booking::getStatus, BookingStatus.APPROVED);
                wrapper.set(Booking::getApproverId, approverId);
                wrapper.set(Booking::getApprovedAt, LocalDateTime.now());
            } else {
                if (rejectReason == null || rejectReason.isBlank()) {
                    throw new BusinessException("REJECT_REASON_REQUIRED", "拒绝时必须填写原因");
                }
                wrapper.set(Booking::getStatus, BookingStatus.REJECTED);
                wrapper.set(Booking::getRejectReason, rejectReason);
                wrapper.set(Booking::getApproverId, approverId);
                wrapper.set(Booking::getApprovedAt, LocalDateTime.now());
            }

            bookingMapper.update(wrapper);

            Booking updated = bookingMapper.selectById(id);
            BookingVO vo = toBookingVO(updated);
            populateJoinedFields(Collections.singletonList(vo));
            return vo;
        } finally {
            lock.unlock();
        }
    }

    @Transactional
    public void cancelBooking(Long id, Long userId) {
        Booking booking = bookingMapper.selectById(id);
        if (booking == null) {
            throw new BusinessException("NOT_FOUND", "预约不存在");
        }
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("FORBIDDEN", "只能取消自己的预约");
        }
        if (booking.getStatus() != BookingStatus.PENDING
                && booking.getStatus() != BookingStatus.APPROVED) {
            throw new BusinessException("BOOKING_NOT_CANCELLABLE", "当前状态不允许取消");
        }

        LambdaUpdateWrapper<Booking> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Booking::getId, id).set(Booking::getStatus, BookingStatus.CANCELLED);
        bookingMapper.update(wrapper);
    }

    @Transactional
    public void completeBooking(Long id) {
        Booking booking = bookingMapper.selectById(id);
        if (booking == null) {
            throw new BusinessException("NOT_FOUND", "预约不存在");
        }
        if (booking.getStatus() != BookingStatus.APPROVED) {
            throw new BusinessException("BOOKING_NOT_COMPLETED", "只有已批准的预约可以确认完成");
        }

        LambdaUpdateWrapper<Booking> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(Booking::getId, id)
                .set(Booking::getStatus, BookingStatus.COMPLETED)
                .set(Booking::getCompletedAt, LocalDateTime.now());
        bookingMapper.update(wrapper);
    }

    public List<TimeSlot> getAvailableSlots(Long labId, String dateStr) {
        LocalDate date = LocalDate.parse(dateStr);
        int dayOfWeek = date.getDayOfWeek().getValue();

        Lab lab = labMapper.selectById(labId);
        if (lab == null) {
            throw new BusinessException("NOT_FOUND", "实验室不存在");
        }

        if (lab.getStatus() != com.labreserve.enums.LabStatus.AVAILABLE) {
            return Collections.emptyList();
        }

        List<LabHours> openHours = labHoursMapper.selectList(
                new LambdaQueryWrapper<LabHours>()
                        .eq(LabHours::getLabId, labId)
                        .eq(LabHours::getDayOfWeek, dayOfWeek));

        List<Booking> conflictingBookings = bookingMapper.selectList(
                new LambdaQueryWrapper<Booking>()
                        .eq(Booking::getLabId, labId)
                        .eq(Booking::getDate, date)
                        .in(Booking::getStatus, BookingStatus.PENDING, BookingStatus.APPROVED));

        List<Course> courses = courseMapper.selectList(
                new LambdaQueryWrapper<Course>()
                        .eq(Course::getLabId, labId)
                        .le(Course::getStartDate, date)
                        .ge(Course::getEndDate, date)
                        .eq(Course::getDayOfWeek, dayOfWeek));

        List<TimeSlot> slots = new ArrayList<>();
        for (LabHours hours : openHours) {
            String current = hours.getOpenTime();
            while (current.compareTo(hours.getCloseTime()) < 0) {
                int hour = Integer.parseInt(current.substring(0, 2));
                int minute = Integer.parseInt(current.substring(3, 5));
                int endMinute = minute + 60;
                int endHour = hour + endMinute / 60;
                endMinute = endMinute % 60;
                String next = String.format("%02d:%02d", endHour, endMinute);

                if (next.compareTo(hours.getCloseTime()) > 0) {
                    break;
                }

                boolean available = true;
                for (Booking b : conflictingBookings) {
                    if (TimeConflictResolver.isTimeOverlap(current, next,
                            b.getStartTime(), b.getEndTime())) {
                        available = false;
                        break;
                    }
                }
                if (available) {
                    for (Course c : courses) {
                        if (TimeConflictResolver.isTimeOverlap(current, next,
                                c.getStartTime(), c.getEndTime())) {
                            available = false;
                            break;
                        }
                    }
                }

                slots.add(TimeSlot.builder()
                        .startTime(current)
                        .endTime(next)
                        .available(available)
                        .build());

                current = next;
            }
        }

        return slots;
    }

    private BookingVO toBookingVO(Booking booking) {
        return BookingVO.builder()
                .id(booking.getId())
                .labId(booking.getLabId())
                .userId(booking.getUserId())
                .date(booking.getDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .purpose(booking.getPurpose())
                .personCount(booking.getPersonCount())
                .status(booking.getStatus().name())
                .rejectReason(booking.getRejectReason())
                .approverId(booking.getApproverId())
                .approvedAt(booking.getApprovedAt())
                .completedAt(booking.getCompletedAt())
                .createdAt(booking.getCreatedAt())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }

    private void populateJoinedFields(List<BookingVO> vos) {
        if (vos.isEmpty()) {
            return;
        }

        Set<Long> labIds = vos.stream()
                .map(BookingVO::getLabId)
                .collect(Collectors.toSet());
        Set<Long> userIds = vos.stream()
                .map(BookingVO::getUserId)
                .collect(Collectors.toSet());
        Set<Long> approverIds = vos.stream()
                .map(BookingVO::getApproverId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        Map<Long, String> labIdToName = Collections.emptyMap();
        if (!labIds.isEmpty()) {
            List<Lab> labs = labMapper.selectBatchIds(labIds);
            labIdToName = labs.stream()
                    .collect(Collectors.toMap(Lab::getId, Lab::getName));
        }

        Map<Long, String> userIdToName = Collections.emptyMap();
        Set<Long> allUserIds = new java.util.HashSet<>(userIds);
        allUserIds.addAll(approverIds);
        if (!allUserIds.isEmpty()) {
            List<User> users = userMapper.selectBatchIds(allUserIds);
            userIdToName = users.stream()
                    .collect(Collectors.toMap(User::getId, User::getRealName));
        }

        for (BookingVO vo : vos) {
            vo.setLabName(labIdToName.get(vo.getLabId()));
            vo.setUserName(userIdToName.get(vo.getUserId()));
            if (vo.getApproverId() != null) {
                vo.setApproverName(userIdToName.get(vo.getApproverId()));
            }
        }
    }
}
