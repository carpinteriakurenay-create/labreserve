package com.labreserve.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.labreserve.dto.DashboardKpiVO;
import com.labreserve.dto.EquipmentUsageStatVO;
import com.labreserve.dto.LabUsageStatVO;
import com.labreserve.dto.StudentRankingVO;
import com.labreserve.entity.Booking;
import com.labreserve.entity.Borrow;
import com.labreserve.entity.Equipment;
import com.labreserve.entity.Lab;
import com.labreserve.entity.User;
import com.labreserve.enums.BookingStatus;
import com.labreserve.enums.BorrowStatus;
import com.labreserve.mapper.BookingMapper;
import com.labreserve.mapper.BorrowMapper;
import com.labreserve.mapper.EquipmentMapper;
import com.labreserve.mapper.LabMapper;
import com.labreserve.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final BookingMapper bookingMapper;
    private final BorrowMapper borrowMapper;
    private final LabMapper labMapper;
    private final EquipmentMapper equipmentMapper;
    private final UserMapper userMapper;

    public DashboardService(BookingMapper bookingMapper, BorrowMapper borrowMapper,
                            LabMapper labMapper, EquipmentMapper equipmentMapper,
                            UserMapper userMapper) {
        this.bookingMapper = bookingMapper;
        this.borrowMapper = borrowMapper;
        this.labMapper = labMapper;
        this.equipmentMapper = equipmentMapper;
        this.userMapper = userMapper;
    }

    public DashboardKpiVO getKpi(Long userId, String role) {
        LocalDate today = LocalDate.now();
        boolean isStudent = "STUDENT".equals(role);

        LambdaQueryWrapper<Booking> todayCompletedWrapper = new LambdaQueryWrapper<Booking>()
                .eq(Booking::getDate, today)
                .eq(Booking::getStatus, BookingStatus.COMPLETED);
        LambdaQueryWrapper<Borrow> todayBorrowsWrapper = new LambdaQueryWrapper<Borrow>()
                .ge(Borrow::getCreatedAt, today.atStartOfDay())
                .le(Borrow::getCreatedAt, today.plusDays(1).atStartOfDay());
        LambdaQueryWrapper<Booking> pendingWrapper = new LambdaQueryWrapper<Booking>()
                .eq(Booking::getStatus, BookingStatus.PENDING);

        if (isStudent) {
            todayCompletedWrapper.eq(Booking::getUserId, userId);
            todayBorrowsWrapper.eq(Borrow::getUserId, userId);
            pendingWrapper.eq(Booking::getUserId, userId);
        }

        long todayBookings = bookingMapper.selectCount(todayCompletedWrapper);
        long todayBorrows = borrowMapper.selectCount(todayBorrowsWrapper);
        long pendingApprovals = bookingMapper.selectCount(pendingWrapper);

        // Lab usage rate: count labs with at least one COMPLETED booking today / total labs
        double labUsageRate = 0.0;
        List<Lab> allLabs = labMapper.selectList(null);
        if (!allLabs.isEmpty()) {
            List<Booking> todayCompletedBookings = bookingMapper.selectList(todayCompletedWrapper);
            long labsWithBookings = todayCompletedBookings.stream()
                    .map(Booking::getLabId)
                    .distinct()
                    .count();
            labUsageRate = (double) labsWithBookings / allLabs.size();
        }

        return DashboardKpiVO.builder()
                .todayBookings(todayBookings)
                .todayBorrows(todayBorrows)
                .labUsageRate(labUsageRate)
                .pendingApprovals(pendingApprovals)
                .build();
    }

    public List<LabUsageStatVO> getLabUsage(Long userId, String role, String dateFrom, String dateTo) {
        boolean isStudent = "STUDENT".equals(role);
        LocalDate from = dateFrom != null ? LocalDate.parse(dateFrom) : LocalDate.now().minusDays(30);
        LocalDate to = dateTo != null ? LocalDate.parse(dateTo) : LocalDate.now();

        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<Booking>()
                .eq(Booking::getStatus, BookingStatus.COMPLETED)
                .ge(Booking::getDate, from)
                .le(Booking::getDate, to);
        if (isStudent) {
            wrapper.eq(Booking::getUserId, userId);
        }

        List<Booking> bookings = bookingMapper.selectList(wrapper);
        Map<Long, List<Booking>> byLab = bookings.stream()
                .collect(Collectors.groupingBy(Booking::getLabId));

        List<Lab> labs = labMapper.selectList(null);
        Map<Long, String> labIdToName = labs.stream()
                .collect(Collectors.toMap(Lab::getId, Lab::getName));

        List<LabUsageStatVO> stats = new ArrayList<>();
        for (Map.Entry<Long, List<Booking>> entry : byLab.entrySet()) {
            Long labId = entry.getKey();
            List<Booking> labBookings = entry.getValue();
            long bookingCount = labBookings.size();
            double usageHours = labBookings.stream()
                    .mapToDouble(b -> {
                        String start = b.getStartTime();
                        String end = b.getEndTime();
                        int startMin = Integer.parseInt(start.substring(0, 2)) * 60 + Integer.parseInt(start.substring(3, 5));
                        int endMin = Integer.parseInt(end.substring(0, 2)) * 60 + Integer.parseInt(end.substring(3, 5));
                        return (endMin - startMin) / 60.0;
                    })
                    .sum();

            stats.add(LabUsageStatVO.builder()
                    .labId(labId)
                    .labName(labIdToName.getOrDefault(labId, ""))
                    .bookingCount(bookingCount)
                    .usageHours(usageHours)
                    .utilizationRate(0.0)
                    .build());
        }

        return stats;
    }

    public List<EquipmentUsageStatVO> getEquipmentUsage(Long userId, String role, String dateFrom, String dateTo) {
        boolean isStudent = "STUDENT".equals(role);
        LocalDate from = dateFrom != null ? LocalDate.parse(dateFrom) : LocalDate.now().minusDays(30);
        LocalDate to = dateTo != null ? LocalDate.parse(dateTo) : LocalDate.now();

        LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<Borrow>()
                .ge(Borrow::getBorrowDate, from)
                .le(Borrow::getBorrowDate, to);
        if (isStudent) {
            wrapper.eq(Borrow::getUserId, userId);
        }

        List<Borrow> borrows = borrowMapper.selectList(wrapper);
        Map<Long, List<Borrow>> byEquipment = borrows.stream()
                .collect(Collectors.groupingBy(Borrow::getEquipmentId));

        List<Equipment> equipments = equipmentMapper.selectList(null);
        Map<Long, String> equipIdToName = equipments.stream()
                .collect(Collectors.toMap(Equipment::getId, Equipment::getName));

        List<EquipmentUsageStatVO> stats = new ArrayList<>();
        for (Map.Entry<Long, List<Borrow>> entry : byEquipment.entrySet()) {
            Long equipmentId = entry.getKey();
            List<Borrow> equipBorrows = entry.getValue();
            long borrowCount = equipBorrows.size();
            double avgBorrowDays = equipBorrows.stream()
                    .filter(b -> b.getActualReturn() != null)
                    .mapToLong(b -> ChronoUnit.DAYS.between(b.getBorrowDate(), b.getActualReturn()))
                    .average()
                    .orElse(0.0);

            stats.add(EquipmentUsageStatVO.builder()
                    .equipmentId(equipmentId)
                    .equipmentName(equipIdToName.getOrDefault(equipmentId, ""))
                    .borrowCount(borrowCount)
                    .avgBorrowDays(avgBorrowDays)
                    .build());
        }

        return stats;
    }

    public List<StudentRankingVO> getStudentRanking(String dateFrom, String dateTo, int limit) {
        LocalDate from = dateFrom != null ? LocalDate.parse(dateFrom) : LocalDate.now().minusDays(30);
        LocalDate to = dateTo != null ? LocalDate.parse(dateTo) : LocalDate.now();

        List<Booking> bookings = bookingMapper.selectList(
                new LambdaQueryWrapper<Booking>()
                        .eq(Booking::getStatus, BookingStatus.COMPLETED)
                        .ge(Booking::getDate, from)
                        .le(Booking::getDate, to));

        Map<Long, List<Booking>> byUser = bookings.stream()
                .collect(Collectors.groupingBy(Booking::getUserId));

        List<User> users = userMapper.selectBatchIds(byUser.keySet());
        Map<Long, String> userIdToName = users.stream()
                .collect(Collectors.toMap(User::getId, User::getRealName));

        return byUser.entrySet().stream()
                .map(entry -> {
                    List<Booking> userBookings = entry.getValue();
                    double totalHours = userBookings.stream()
                            .mapToDouble(b -> {
                                String start = b.getStartTime();
                                String end = b.getEndTime();
                                int startMin = Integer.parseInt(start.substring(0, 2)) * 60 + Integer.parseInt(start.substring(3, 5));
                                int endMin = Integer.parseInt(end.substring(0, 2)) * 60 + Integer.parseInt(end.substring(3, 5));
                                return (endMin - startMin) / 60.0;
                            })
                            .sum();

                    return StudentRankingVO.builder()
                            .userId(entry.getKey())
                            .userRealName(userIdToName.getOrDefault(entry.getKey(), ""))
                            .bookingCount(userBookings.size())
                            .totalHours(totalHours)
                            .build();
                })
                .sorted(Comparator.comparingLong(StudentRankingVO::getBookingCount).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }
}
