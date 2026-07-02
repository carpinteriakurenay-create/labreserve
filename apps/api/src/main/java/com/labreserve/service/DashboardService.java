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
import com.labreserve.mapper.BookingMapper;
import com.labreserve.mapper.BorrowMapper;
import com.labreserve.mapper.EquipmentMapper;
import com.labreserve.mapper.EquipmentUsageAggRow;
import com.labreserve.mapper.LabMapper;
import com.labreserve.mapper.LabUsageAggRow;
import com.labreserve.mapper.UserMapper;
import com.labreserve.mapper.UserRankingAggRow;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    @Cacheable(value = "kpi", key = "#userId + ':' + #role")
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

        // Use COUNT(DISTINCT lab_id) from SQL instead of loading all records
        long totalLabs = labMapper.selectCount(null);
        double labUsageRate = 0.0;
        if (totalLabs > 0) {
            long labsWithBookings = bookingMapper.countDistinctLabsByDate(today.toString());
            labUsageRate = (double) labsWithBookings / totalLabs;
        }

        return DashboardKpiVO.builder()
                .todayBookings(todayBookings)
                .todayBorrows(todayBorrows)
                .labUsageRate(labUsageRate)
                .pendingApprovals(pendingApprovals)
                .build();
    }

    @Cacheable(value = "labUsage", key = "#userId + ':' + #role + ':' + #dateFrom + ':' + #dateTo")
    public List<LabUsageStatVO> getLabUsage(Long userId, String role, String dateFrom, String dateTo) {
        boolean isStudent = "STUDENT".equals(role);
        LocalDate from = dateFrom != null ? LocalDate.parse(dateFrom) : LocalDate.now().minusDays(30);
        LocalDate to = dateTo != null ? LocalDate.parse(dateTo) : LocalDate.now();

        // For student: individual user bookings are small — use paginated approach
        if (isStudent) {
            return getLabUsageForStudent(userId, from, to);
        }

        // For teacher/admin: use SQL aggregation to avoid loading all rows
        List<LabUsageAggRow> rows = bookingMapper.aggregateLabUsage(from.toString(), to.toString());
        if (rows.isEmpty()) return Collections.emptyList();

        Set<Long> labIds = rows.stream().map(LabUsageAggRow::getLabId).collect(Collectors.toSet());
        Map<Long, String> labIdToName = labMapper.selectBatchIds(labIds).stream()
                .collect(Collectors.toMap(Lab::getId, Lab::getName));

        return rows.stream().map(r -> LabUsageStatVO.builder()
                .labId(r.getLabId())
                .labName(labIdToName.getOrDefault(r.getLabId(), ""))
                .bookingCount(r.getBookingCount() != null ? r.getBookingCount() : 0L)
                .usageHours(r.getUsageHours() != null ? r.getUsageHours() : 0.0)
                .utilizationRate(0.0)
                .build()).collect(Collectors.toList());
    }

    private List<LabUsageStatVO> getLabUsageForStudent(Long userId, LocalDate from, LocalDate to) {
        // Student data is tiny — paginated select is fine
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<Booking>()
                .eq(Booking::getUserId, userId)
                .eq(Booking::getStatus, BookingStatus.COMPLETED)
                .ge(Booking::getDate, from)
                .le(Booking::getDate, to);
        List<Booking> bookings = bookingMapper.selectList(wrapper);
        Map<Long, List<Booking>> byLab = bookings.stream()
                .collect(Collectors.groupingBy(Booking::getLabId));

        Set<Long> labIds = byLab.keySet();
        Map<Long, String> labIdToName = labIds.isEmpty() ? Collections.emptyMap()
                : labMapper.selectBatchIds(labIds).stream()
                .collect(Collectors.toMap(Lab::getId, Lab::getName));

        List<LabUsageStatVO> stats = new ArrayList<>();
        for (Map.Entry<Long, List<Booking>> entry : byLab.entrySet()) {
            List<Booking> labBookings = entry.getValue();
            double usageHours = labBookings.stream()
                    .mapToDouble(b -> {
                        int startMin = Integer.parseInt(b.getStartTime().substring(0, 2)) * 60
                                + Integer.parseInt(b.getStartTime().substring(3, 5));
                        int endMin = Integer.parseInt(b.getEndTime().substring(0, 2)) * 60
                                + Integer.parseInt(b.getEndTime().substring(3, 5));
                        return (endMin - startMin) / 60.0;
                    }).sum();
            stats.add(LabUsageStatVO.builder()
                    .labId(entry.getKey())
                    .labName(labIdToName.getOrDefault(entry.getKey(), ""))
                    .bookingCount((long) labBookings.size())
                    .usageHours(usageHours)
                    .utilizationRate(0.0)
                    .build());
        }
        return stats;
    }

    @Cacheable(value = "equipmentUsage", key = "#userId + ':' + #role + ':' + #dateFrom + ':' + #dateTo")
    public List<EquipmentUsageStatVO> getEquipmentUsage(Long userId, String role, String dateFrom, String dateTo) {
        boolean isStudent = "STUDENT".equals(role);
        LocalDate from = dateFrom != null ? LocalDate.parse(dateFrom) : LocalDate.now().minusDays(30);
        LocalDate to = dateTo != null ? LocalDate.parse(dateTo) : LocalDate.now();

        // For student: use paginated approach (tiny data)
        if (isStudent) {
            LambdaQueryWrapper<Borrow> wrapper = new LambdaQueryWrapper<Borrow>()
                    .eq(Borrow::getUserId, userId)
                    .ge(Borrow::getBorrowDate, from)
                    .le(Borrow::getBorrowDate, to);
            List<Borrow> borrows = borrowMapper.selectList(wrapper);
            return buildEquipmentUsageStats(borrows);
        }

        // For teacher/admin: use SQL aggregation
        List<EquipmentUsageAggRow> rows = borrowMapper.aggregateEquipmentUsage(from.toString(), to.toString());
        if (rows.isEmpty()) return Collections.emptyList();

        Set<Long> equipIds = rows.stream().map(EquipmentUsageAggRow::getEquipmentId).collect(Collectors.toSet());
        Map<Long, String> idToName = equipmentMapper.selectBatchIds(equipIds).stream()
                .collect(Collectors.toMap(Equipment::getId, Equipment::getName));

        return rows.stream().map(r -> EquipmentUsageStatVO.builder()
                .equipmentId(r.getEquipmentId())
                .equipmentName(idToName.getOrDefault(r.getEquipmentId(), ""))
                .borrowCount(r.getBorrowCount() != null ? r.getBorrowCount() : 0L)
                .avgBorrowDays(r.getAvgBorrowDays() != null ? r.getAvgBorrowDays() : 0.0)
                .build()).collect(Collectors.toList());
    }

    private List<EquipmentUsageStatVO> buildEquipmentUsageStats(List<Borrow> borrows) {
        Map<Long, List<Borrow>> byEquip = borrows.stream()
                .collect(Collectors.groupingBy(Borrow::getEquipmentId));
        Set<Long> equipIds = byEquip.keySet();
        Map<Long, String> idToName = equipIds.isEmpty() ? Collections.emptyMap()
                : equipmentMapper.selectBatchIds(equipIds).stream()
                .collect(Collectors.toMap(Equipment::getId, Equipment::getName));

        List<EquipmentUsageStatVO> stats = new ArrayList<>();
        for (Map.Entry<Long, List<Borrow>> entry : byEquip.entrySet()) {
            List<Borrow> equipBorrows = entry.getValue();
            double avgDays = equipBorrows.stream()
                    .filter(b -> b.getActualReturn() != null)
                    .mapToLong(b -> java.time.temporal.ChronoUnit.DAYS.between(b.getBorrowDate(), b.getActualReturn()))
                    .average().orElse(0.0);
            stats.add(EquipmentUsageStatVO.builder()
                    .equipmentId(entry.getKey())
                    .equipmentName(idToName.getOrDefault(entry.getKey(), ""))
                    .borrowCount((long) equipBorrows.size())
                    .avgBorrowDays(avgDays)
                    .build());
        }
        return stats;
    }

    @Cacheable(value = "studentRanking", key = "#dateFrom + ':' + #dateTo + ':' + #limit")
    public List<StudentRankingVO> getStudentRanking(String dateFrom, String dateTo, int limit) {
        LocalDate from = dateFrom != null ? LocalDate.parse(dateFrom) : LocalDate.now().minusDays(30);
        LocalDate to = dateTo != null ? LocalDate.parse(dateTo) : LocalDate.now();

        List<UserRankingAggRow> rows = bookingMapper.aggregateUserRanking(from.toString(), to.toString(), limit);
        if (rows.isEmpty()) return Collections.emptyList();

        Set<Long> userIds = rows.stream().map(UserRankingAggRow::getUserId).collect(Collectors.toSet());
        Map<Long, String> idToName = userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getRealName));

        return rows.stream().map(r -> StudentRankingVO.builder()
                .userId(r.getUserId())
                .userRealName(idToName.getOrDefault(r.getUserId(), ""))
                .bookingCount(r.getBookingCount() != null ? r.getBookingCount() : 0L)
                .totalHours(r.getTotalHours() != null ? r.getTotalHours() : 0.0)
                .build()).collect(Collectors.toList());
    }
}
