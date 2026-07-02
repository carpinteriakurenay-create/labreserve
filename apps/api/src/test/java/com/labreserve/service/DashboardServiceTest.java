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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
            when(bookingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(5L, 2L);
            when(borrowMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(3L);
            when(labMapper.selectCount(isNull())).thenReturn(2L);
            when(bookingMapper.countDistinctLabsByDate(anyString())).thenReturn(1L);

            DashboardKpiVO kpi = dashboardService.getKpi(1L, "STUDENT");

            assertNotNull(kpi);
            assertEquals(5L, kpi.getTodayBookings());
            assertEquals(3L, kpi.getTodayBorrows());
            assertEquals(2L, kpi.getPendingApprovals());
            assertEquals(0.5, kpi.getLabUsageRate());
        }

        @Test
        void shouldReturnKpiForAdmin() {
            when(bookingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(10L, 5L);
            when(borrowMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(8L);
            when(labMapper.selectCount(isNull())).thenReturn(3L);
            when(bookingMapper.countDistinctLabsByDate(anyString())).thenReturn(2L);

            DashboardKpiVO kpi = dashboardService.getKpi(3L, "ADMIN");

            assertNotNull(kpi);
            assertEquals(10L, kpi.getTodayBookings());
            assertEquals(8L, kpi.getTodayBorrows());
            assertEquals(5L, kpi.getPendingApprovals());
            assertEquals(2.0 / 3.0, kpi.getLabUsageRate());
        }

        @Test
        void shouldReturnZeroRateWhenNoLabs() {
            when(bookingMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(borrowMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(0L);
            when(labMapper.selectCount(isNull())).thenReturn(0L);

            DashboardKpiVO kpi = dashboardService.getKpi(1L, "STUDENT");

            assertEquals(0.0, kpi.getLabUsageRate());
        }
    }

    @Nested
    class GetStudentRanking {

        @Test
        void shouldReturnRankingSortedByBookingCount() {
            UserRankingAggRow r1 = new UserRankingAggRow();
            r1.setUserId(1L);
            r1.setBookingCount(2L);
            r1.setTotalHours(4.0);

            UserRankingAggRow r2 = new UserRankingAggRow();
            r2.setUserId(2L);
            r2.setBookingCount(1L);
            r2.setTotalHours(1.0);

            when(bookingMapper.aggregateUserRanking(anyString(), anyString(), eq(10)))
                    .thenReturn(List.of(r1, r2));

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
            assertEquals(2L, ranking.get(0).getBookingCount());
        }

        @Test
        void shouldRespectLimit() {
            UserRankingAggRow r = new UserRankingAggRow();
            r.setUserId(1L);
            r.setBookingCount(1L);
            r.setTotalHours(1.0);
            when(bookingMapper.aggregateUserRanking(anyString(), anyString(), eq(1)))
                    .thenReturn(List.of(r));

            User u = new User();
            u.setId(1L);
            u.setRealName("Alice");
            when(userMapper.selectBatchIds(anySet())).thenReturn(List.of(u));

            List<StudentRankingVO> ranking = dashboardService.getStudentRanking(null, null, 1);

            assertEquals(1, ranking.size());
        }

        @Test
        void shouldReturnEmptyWhenNoData() {
            when(bookingMapper.aggregateUserRanking(anyString(), anyString(), eq(10)))
                    .thenReturn(Collections.emptyList());

            List<StudentRankingVO> ranking = dashboardService.getStudentRanking(null, null, 10);

            assertTrue(ranking.isEmpty());
        }
    }
}
