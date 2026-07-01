package com.labreserve.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.labreserve.dto.UsageRecordVO;
import com.labreserve.entity.Booking;
import com.labreserve.entity.Lab;
import com.labreserve.entity.User;
import com.labreserve.enums.BookingStatus;
import com.labreserve.mapper.BookingMapper;
import com.labreserve.mapper.LabMapper;
import com.labreserve.mapper.UserMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UsageRecordService {

    private final BookingMapper bookingMapper;
    private final LabMapper labMapper;
    private final UserMapper userMapper;

    public UsageRecordService(BookingMapper bookingMapper, LabMapper labMapper, UserMapper userMapper) {
        this.bookingMapper = bookingMapper;
        this.labMapper = labMapper;
        this.userMapper = userMapper;
    }

    public IPage<UsageRecordVO> listUsageRecords(Long labId, Long userId,
                                                  String dateFrom, String dateTo,
                                                  int pageNum, int pageSize) {
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Booking::getStatus, BookingStatus.COMPLETED);

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

        wrapper.orderByDesc(Booking::getCompletedAt);

        Page<Booking> page = new Page<>(pageNum, pageSize);
        IPage<Booking> result = bookingMapper.selectPage(page, wrapper);

        Map<Long, String> labIdToName = loadLabNames(result.getRecords());
        Map<Long, String> userIdToName = loadUserNames(result.getRecords());

        IPage<UsageRecordVO> voPage = result.convert(b -> toUsageRecordVO(b, labIdToName, userIdToName));
        return voPage;
    }

    public String exportCsv(Long labId, Long userId, String dateFrom, String dateTo) {
        LambdaQueryWrapper<Booking> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Booking::getStatus, BookingStatus.COMPLETED);

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

        wrapper.orderByDesc(Booking::getCompletedAt);

        List<Booking> bookings = bookingMapper.selectList(wrapper);
        Map<Long, String> labIdToName = loadLabNames(bookings);
        Map<Long, String> userIdToName = loadUserNames(bookings);

        StringBuilder sb = new StringBuilder();
        sb.append('﻿'); // UTF-8 BOM for Chinese Excel compatibility
        sb.append("ID,实验室名称,用户姓名,日期,开始时间,结束时间,用途,人数,完成时间\n");

        for (Booking b : bookings) {
            UsageRecordVO vo = toUsageRecordVO(b, labIdToName, userIdToName);
            sb.append(vo.getId()).append(',');
            sb.append(csvEscape(vo.getLabName())).append(',');
            sb.append(csvEscape(vo.getUserRealName())).append(',');
            sb.append(vo.getDate()).append(',');
            sb.append(vo.getStartTime()).append(',');
            sb.append(vo.getEndTime()).append(',');
            sb.append(csvEscape(vo.getPurpose() != null ? vo.getPurpose() : "")).append(',');
            sb.append(vo.getPersonCount()).append(',');
            sb.append(vo.getCompletedAt()).append('\n');
        }

        return sb.toString();
    }

    private String csvEscape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private UsageRecordVO toUsageRecordVO(Booking booking, Map<Long, String> labIdToName,
                                           Map<Long, String> userIdToName) {
        return UsageRecordVO.builder()
                .id(booking.getId())
                .bookingId(booking.getId())
                .labId(booking.getLabId())
                .labName(labIdToName.getOrDefault(booking.getLabId(), ""))
                .userId(booking.getUserId())
                .userRealName(userIdToName.getOrDefault(booking.getUserId(), ""))
                .date(booking.getDate().toString())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .purpose(booking.getPurpose())
                .personCount(booking.getPersonCount())
                .completedAt(booking.getCompletedAt() != null ? booking.getCompletedAt().toString() : "")
                .build();
    }

    private Map<Long, String> loadLabNames(List<Booking> bookings) {
        Set<Long> labIds = bookings.stream().map(Booking::getLabId).collect(Collectors.toSet());
        if (labIds.isEmpty()) return Collections.emptyMap();
        return labMapper.selectBatchIds(labIds).stream()
                .collect(Collectors.toMap(Lab::getId, Lab::getName));
    }

    private Map<Long, String> loadUserNames(List<Booking> bookings) {
        Set<Long> userIds = bookings.stream().map(Booking::getUserId).collect(Collectors.toSet());
        if (userIds.isEmpty()) return Collections.emptyMap();
        return userMapper.selectBatchIds(userIds).stream()
                .collect(Collectors.toMap(User::getId, User::getRealName));
    }
}
