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
import com.labreserve.mapper.LabMapper;
import com.labreserve.mapper.UserMapper;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private BookingMapper bookingMapper;
    @Mock private BorrowMapper borrowMapper;
    @Mock private LabMapper labMapper;
    @Mock private EquipmentMapper equipmentMapper;
    @Mock private UserMapper userMapper;

    @InjectMocks
    private DashboardService dashboardService;

    @Nested
    class GetKpi {

        @Test
        void shouldReturnKpiForStudent() {
            Lab lab = new Lab();
            lab.setId(1L);
            lab.setName("Lab 1");

            when(bookingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L, 2L);
            when(borrowMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
            when(labMapper.selectList(isNull())).thenReturn(List.of(lab));
            when(bookingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(
                    List.of(createBooking(1L, 1L)));

            DashboardKpiVO kpi = dashboardService.getKpi(1L, "STUDENT");

            assertNotNull(kpi);
            assertEquals(5L, kpi.getTodayBookings());
            assertEquals(3L, kpi.getTodayBorrows());
            assertEquals(2L, kpi.getPendingApprovals());
            assertTrue(kpi.getLabUsageRate() >= 0.0);
        }

        @Test
        void shouldReturnKpiForAdmin() {
            Lab lab = new Lab();
            lab.setId(1L);
            lab.setName("Lab 1");

            when(bookingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L, 5L);
            when(borrowMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(8L);
            when(labMapper.selectList(isNull())).thenReturn(List.of(lab, new Lab()));
            when(bookingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(
                    List.of(createBooking(1L, 1L)));

            DashboardKpiVO kpi = dashboardService.getKpi(3L, "ADMIN");

            assertNotNull(kpi);
            assertEquals(10L, kpi.getTodayBookings());
            assertEquals(8L, kpi.getTodayBorrows());
            assertEquals(5L, kpi.getPendingApprovals());
        }

        @Test
        void shouldReturnZeroRateWhenNoLabs() {
            when(bookingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(borrowMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(labMapper.selectList(isNull())).thenReturn(Collections.emptyList());

            DashboardKpiVO kpi = dashboardService.getKpi(1L, "STUDENT");

            assertEquals(0.0, kpi.getLabUsageRate());
        }

        private Booking createBooking(Long id, Long labId) {
            Booking booking = new Booking();
            booking.setId(id);
            booking.setLabId(labId);
            booking.setStatus(BookingStatus.COMPLETED);
            return booking;
        }
    }

    @Nested
    class GetStudentRanking {

        @Test
        void shouldReturnRankingSortedByBookingCount() {
            Booking b1 = new Booking();
            b1.setUserId(1L);
            b1.setStartTime("08:00");
            b1.setEndTime("10:00");
            b1.setStatus(BookingStatus.COMPLETED);

            Booking b2 = new Booking();
            b2.setUserId(2L);
            b2.setStartTime("08:00");
            b2.setEndTime("09:00");
            b2.setStatus(BookingStatus.COMPLETED);

            Booking b3 = new Booking();
            b3.setUserId(1L);
            b3.setStartTime("14:00");
            b3.setEndTime("16:00");
            b3.setStatus(BookingStatus.COMPLETED);

            when(bookingMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(b1, b2, b3));

            User u1 = new User();
            u1.setId(1L);
            u1.setRealName("Alice");
            User u2 = new User();
            u2.setId(2L);
            u2.setRealName("Bob");
            when(userMapper.selectBatchIds(anySet())).thenReturn(List.of(u1, u2));

            List<StudentRankingVO> ranking = dashboardService.getStudentRanking(null, null, 10);

            assertFalse(ranking.isEmpty());
            assertEquals(1L, ranking.get(0).getUserId());
            assertEquals(2, ranking.get(0).getBookingCount());
        }

        @Test
        void shouldRespectLimit() {
            Booking b = new Booking();
            b.setUserId(1L);
            b.setStartTime("08:00");
            b.setEndTime("09:00");
            b.setStatus(BookingStatus.COMPLETED);
            when(bookingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(b));

            User u = new User();
            u.setId(1L);
            u.setRealName("Alice");
            when(userMapper.selectBatchIds(anySet())).thenReturn(List.of(u));

            List<StudentRankingVO> ranking = dashboardService.getStudentRanking(null, null, 1);

            assertEquals(1, ranking.size());
        }
    }
}
