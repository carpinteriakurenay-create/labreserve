package com.labreserve.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.labreserve.dto.BookingCreateRequest;
import com.labreserve.dto.BookingVO;
import com.labreserve.dto.TimeSlot;
import com.labreserve.entity.Booking;
import com.labreserve.entity.Course;
import com.labreserve.entity.Lab;
import com.labreserve.entity.LabHours;
import com.labreserve.enums.BookingStatus;
import com.labreserve.enums.LabStatus;
import com.labreserve.exception.BusinessException;
import com.labreserve.mapper.BookingMapper;
import com.labreserve.mapper.CourseMapper;
import com.labreserve.mapper.LabHoursMapper;
import com.labreserve.mapper.LabMapper;
import com.labreserve.mapper.UserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BookingServiceTest {

    @Mock private BookingMapper bookingMapper;
    @Mock private LabMapper labMapper;
    @Mock private LabHoursMapper labHoursMapper;
    @Mock private CourseMapper courseMapper;
    @Mock private UserMapper userMapper;
    @Mock private RedissonClient redissonClient;
    @Mock private RLock rLock;

    @InjectMocks
    private BookingService bookingService;

    private Lab testLab;
    private List<LabHours> testOpenHours;

    @BeforeEach
    void setUp() throws InterruptedException {
        testLab = new Lab();
        testLab.setId(1L);
        testLab.setName("Test Lab");
        testLab.setStatus(LabStatus.AVAILABLE);

        testOpenHours = List.of(
                createLabHours(1L, 1L, "08:00", "18:00"));

        lenient().when(redissonClient.getLock(anyString())).thenReturn(rLock);
        lenient().when(rLock.tryLock(anyLong(), any(TimeUnit.class))).thenReturn(true);
    }

    private LabHours createLabHours(Long id, Long labId, String openTime, String closeTime) {
        LabHours hours = new LabHours();
        hours.setId(id);
        hours.setLabId(labId);
        hours.setDayOfWeek(LocalDate.now().getDayOfWeek().getValue());
        hours.setOpenTime(openTime);
        hours.setCloseTime(closeTime);
        return hours;
    }

    @Nested
    class CreateBooking {

        @Test
        void shouldCreateBookingSuccessfully() {
            BookingCreateRequest request = new BookingCreateRequest();
            request.setLabId(1L);
            request.setDate(LocalDate.now().toString());
            request.setStartTime("10:00");
            request.setEndTime("12:00");
            request.setPurpose("Test");
            request.setPersonCount(2);

            when(labMapper.selectById(1L)).thenReturn(testLab);
            when(labHoursMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testOpenHours);
            when(bookingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(courseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            BookingVO result = bookingService.createBooking(request, 1L);

            assertNotNull(result);
            assertEquals(1L, result.getLabId());
            assertEquals(1L, result.getUserId());
            assertEquals("Test", result.getPurpose());
            assertEquals(2, result.getPersonCount());
            assertEquals("PENDING", result.getStatus());
        }

        @Test
        void shouldThrowWhenLabNotFound() {
            BookingCreateRequest request = new BookingCreateRequest();
            request.setLabId(999L);
            request.setDate(LocalDate.now().toString());
            request.setStartTime("10:00");
            request.setEndTime("12:00");

            when(labMapper.selectById(999L)).thenReturn(null);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bookingService.createBooking(request, 1L));
            assertEquals("NOT_FOUND", ex.getCode());
        }

        @Test
        void shouldThrowWhenLabClosed() {
            testLab.setStatus(LabStatus.CLOSED);
            BookingCreateRequest request = new BookingCreateRequest();
            request.setLabId(1L);
            request.setDate(LocalDate.now().toString());
            request.setStartTime("10:00");
            request.setEndTime("12:00");

            when(labMapper.selectById(1L)).thenReturn(testLab);
            when(labHoursMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testOpenHours);
            when(bookingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(courseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bookingService.createBooking(request, 1L));
            assertEquals("LAB_CLOSED", ex.getCode());
        }

        @Test
        void shouldThrowWhenTimeConflict() {
            BookingCreateRequest request = new BookingCreateRequest();
            request.setLabId(1L);
            request.setDate(LocalDate.now().toString());
            request.setStartTime("10:00");
            request.setEndTime("12:00");

            Booking conflictingBooking = new Booking();
            conflictingBooking.setId(10L);
            conflictingBooking.setLabId(1L);
            conflictingBooking.setDate(LocalDate.now());
            conflictingBooking.setStartTime("11:00");
            conflictingBooking.setEndTime("13:00");
            conflictingBooking.setStatus(BookingStatus.APPROVED);

            when(labMapper.selectById(1L)).thenReturn(testLab);
            when(labHoursMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testOpenHours);
            when(bookingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of(conflictingBooking));
            when(courseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bookingService.createBooking(request, 1L));
            assertEquals("TIME_CONFLICT", ex.getCode());
        }

        @Test
        void shouldThrowWhenOutsideOpenHours() {
            BookingCreateRequest request = new BookingCreateRequest();
            request.setLabId(1L);
            request.setDate(LocalDate.now().toString());
            request.setStartTime("20:00");
            request.setEndTime("22:00");

            when(labMapper.selectById(1L)).thenReturn(testLab);
            when(labHoursMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testOpenHours);
            when(bookingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(courseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bookingService.createBooking(request, 1L));
            assertEquals("OUTSIDE_OPEN_HOURS", ex.getCode());
        }
    }

    @Nested
    class CancelBooking {

        @Test
        void shouldCancelPendingBooking() {
            Booking booking = new Booking();
            booking.setId(1L);
            booking.setUserId(1L);
            booking.setStatus(BookingStatus.PENDING);

            when(bookingMapper.selectById(1L)).thenReturn(booking);

            bookingService.cancelBooking(1L, 1L);

            verify(bookingMapper).update(any());
        }

        @Test
        void shouldCancelApprovedBooking() {
            Booking booking = new Booking();
            booking.setId(1L);
            booking.setUserId(1L);
            booking.setStatus(BookingStatus.APPROVED);

            when(bookingMapper.selectById(1L)).thenReturn(booking);

            bookingService.cancelBooking(1L, 1L);

            verify(bookingMapper).update(any());
        }

        @Test
        void shouldThrowWhenCancellingCompletedBooking() {
            Booking booking = new Booking();
            booking.setId(1L);
            booking.setUserId(1L);
            booking.setStatus(BookingStatus.COMPLETED);

            when(bookingMapper.selectById(1L)).thenReturn(booking);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bookingService.cancelBooking(1L, 1L));
            assertEquals("BOOKING_NOT_CANCELLABLE", ex.getCode());
        }

        @Test
        void shouldThrowWhenCancellingOthersBooking() {
            Booking booking = new Booking();
            booking.setId(1L);
            booking.setUserId(2L);
            booking.setStatus(BookingStatus.PENDING);

            when(bookingMapper.selectById(1L)).thenReturn(booking);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bookingService.cancelBooking(1L, 1L));
            assertEquals("FORBIDDEN", ex.getCode());
        }
    }

    @Nested
    class ApproveBooking {

        @Test
        void shouldApproveBooking() throws InterruptedException {
            Booking booking = new Booking();
            booking.setId(1L);
            booking.setLabId(1L);
            booking.setDate(LocalDate.now());
            booking.setStartTime("10:00");
            booking.setEndTime("12:00");
            booking.setStatus(BookingStatus.PENDING);

            when(bookingMapper.selectById(1L)).thenReturn(booking);
            when(labMapper.selectById(1L)).thenReturn(testLab);
            when(labHoursMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testOpenHours);
            when(bookingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(courseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            BookingVO result = bookingService.approveBooking(1L, true, null, 2L);

            assertNotNull(result);
        }

        @Test
        void shouldRejectBookingWithoutReason() {
            Booking booking = new Booking();
            booking.setId(1L);
            booking.setStatus(BookingStatus.PENDING);

            when(bookingMapper.selectById(1L)).thenReturn(booking);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bookingService.approveBooking(1L, false, "   ", 2L));
            assertEquals("REJECT_REASON_REQUIRED", ex.getCode());
        }

        @Test
        void shouldThrowWhenAlreadyProcessed() {
            Booking booking = new Booking();
            booking.setId(1L);
            booking.setStatus(BookingStatus.APPROVED);

            when(bookingMapper.selectById(1L)).thenReturn(booking);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bookingService.approveBooking(1L, true, null, 2L));
            assertEquals("ALREADY_PROCESSED", ex.getCode());
        }
    }

    @Nested
    class CompleteBooking {

        @Test
        void shouldCompleteApprovedBooking() {
            Booking booking = new Booking();
            booking.setId(1L);
            booking.setStatus(BookingStatus.APPROVED);

            when(bookingMapper.selectById(1L)).thenReturn(booking);

            bookingService.completeBooking(1L);

            verify(bookingMapper).update(any());
        }

        @Test
        void shouldThrowWhenNotApproved() {
            Booking booking = new Booking();
            booking.setId(1L);
            booking.setStatus(BookingStatus.PENDING);

            when(bookingMapper.selectById(1L)).thenReturn(booking);

            BusinessException ex = assertThrows(BusinessException.class,
                    () -> bookingService.completeBooking(1L));
            assertEquals("BOOKING_NOT_COMPLETED", ex.getCode());
        }
    }

    @Nested
    class GetAvailableSlots {

        @Test
        void shouldReturnSlotsForAvailableLab() {
            when(labMapper.selectById(1L)).thenReturn(testLab);
            when(labHoursMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(testOpenHours);
            when(bookingMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());
            when(courseMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(Collections.emptyList());

            List<TimeSlot> slots = bookingService.getAvailableSlots(1L, LocalDate.now().toString());

            assertFalse(slots.isEmpty());
            assertTrue(slots.stream().allMatch(TimeSlot::isAvailable));
        }

        @Test
        void shouldReturnEmptyForClosedLab() {
            testLab.setStatus(LabStatus.CLOSED);
            when(labMapper.selectById(1L)).thenReturn(testLab);

            List<TimeSlot> slots = bookingService.getAvailableSlots(1L, LocalDate.now().toString());

            assertTrue(slots.isEmpty());
        }
    }
}
