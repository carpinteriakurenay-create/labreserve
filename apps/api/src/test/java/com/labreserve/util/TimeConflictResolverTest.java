package com.labreserve.util;

import com.labreserve.dto.BookingCreateRequest;
import com.labreserve.entity.Booking;
import com.labreserve.entity.Course;
import com.labreserve.entity.Lab;
import com.labreserve.entity.LabHours;
import com.labreserve.enums.BookingStatus;
import com.labreserve.enums.LabStatus;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

class TimeConflictResolverTest {

    @Nested
    class IsTimeOverlap {

        @Test
        void shouldDetectFullOverlap() {
            assertTrue(TimeConflictResolver.isTimeOverlap("08:00", "10:00", "08:00", "10:00"));
        }

        @Test
        void shouldDetectPartialOverlap() {
            assertTrue(TimeConflictResolver.isTimeOverlap("09:00", "11:00", "08:00", "10:00"));
        }

        @Test
        void shouldNotOverlapWhenAdjacent() {
            assertFalse(TimeConflictResolver.isTimeOverlap("08:00", "09:00", "09:00", "10:00"));
        }

        @Test
        void shouldDetectContainment() {
            assertTrue(TimeConflictResolver.isTimeOverlap("08:00", "12:00", "09:00", "11:00"));
        }

        @Test
        void shouldNotOverlapWhenDisjoint() {
            assertFalse(TimeConflictResolver.isTimeOverlap("08:00", "09:00", "10:00", "11:00"));
        }

        @Test
        void shouldThrowOnCrossDay() {
            assertThrows(IllegalArgumentException.class,
                    () -> TimeConflictResolver.isTimeOverlap("22:00", "02:00", "08:00", "10:00"));
        }
    }

    @Nested
    class IsWithinOpenHours {

        @Test
        void shouldBeTrueWhenBookingFitsExactly() {
            LabHours hours = new LabHours();
            hours.setOpenTime("08:00");
            hours.setCloseTime("12:00");

            assertTrue(TimeConflictResolver.isWithinOpenHours("08:00", "12:00", List.of(hours)));
        }

        @Test
        void shouldBeTrueWhenBookingIsInside() {
            LabHours hours = new LabHours();
            hours.setOpenTime("08:00");
            hours.setCloseTime("21:00");

            assertTrue(TimeConflictResolver.isWithinOpenHours("14:00", "16:00", List.of(hours)));
        }

        @Test
        void shouldBeFalseWhenBookingExtendsBeyondClose() {
            LabHours hours = new LabHours();
            hours.setOpenTime("08:00");
            hours.setCloseTime("18:00");

            assertFalse(TimeConflictResolver.isWithinOpenHours("17:00", "19:00", List.of(hours)));
        }

        @Test
        void shouldBeFalseWhenNoOpenHours() {
            assertFalse(TimeConflictResolver.isWithinOpenHours("08:00", "10:00", Collections.emptyList()));
        }
    }

    static Stream<Arguments> provideConflictCases() {
        LocalDate today = LocalDate.now();

        return Stream.of(
                Arguments.of("完全重叠", "08:00", "10:00", "08:00", "10:00", today, 1L, true),
                Arguments.of("部分重叠", "09:00", "11:00", "08:00", "10:00", today, 1L, true),
                Arguments.of("首尾相连", "08:00", "09:00", "09:00", "10:00", today, 1L, false),
                Arguments.of("包含", "08:00", "12:00", "09:00", "11:00", today, 1L, true),
                Arguments.of("不同日期", "08:00", "10:00", "08:00", "10:00", today.plusDays(1), 1L, false),
                Arguments.of("不同实验室", "08:00", "10:00", "08:00", "10:00", today, 2L, false),
                Arguments.of("与课程冲突", "08:00", "09:00", null, null, today, 1L, true),
                Arguments.of("与已审批冲突", "08:00", "09:00", "08:00", "10:00", today, 1L, true)
        );
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("provideConflictCases")
    void shouldCheckConflictsCorrectly(
            String caseName,
            String requestStart, String requestEnd,
            String existingStart, String existingEnd,
            LocalDate existingDate,
            Long existingLabId,
            boolean expectConflict) {

        BookingCreateRequest request = new BookingCreateRequest();
        request.setLabId(1L);
        request.setDate(LocalDate.now().toString());
        request.setStartTime(requestStart);
        request.setEndTime(requestEnd);
        request.setPurpose("测试预约");

        Lab lab = new Lab();
        lab.setId(1L);
        lab.setStatus(LabStatus.AVAILABLE);

        LabHours openHours = new LabHours();
        openHours.setDayOfWeek(LocalDate.now().getDayOfWeek().getValue());
        openHours.setOpenTime("06:00");
        openHours.setCloseTime("22:00");

        List<Booking> approvedBookings;
        List<Course> courses;

        if ("与课程冲突".equals(caseName)) {
            Course course = new Course();
            course.setLabId(existingLabId);
            course.setDayOfWeek(existingDate.getDayOfWeek().getValue());
            course.setStartDate(existingDate.minusDays(7));
            course.setEndDate(existingDate.plusDays(7));
            course.setStartTime(requestStart);
            course.setEndTime(requestEnd);
            courses = List.of(course);
            approvedBookings = Collections.emptyList();
        } else if (existingStart != null) {
            Booking existing = new Booking();
            existing.setLabId(existingLabId);
            existing.setDate(existingDate);
            existing.setStartTime(existingStart);
            existing.setEndTime(existingEnd);
            approvedBookings = List.of(existing);
            courses = Collections.emptyList();
        } else {
            approvedBookings = Collections.emptyList();
            courses = Collections.emptyList();
        }

        List<String> conflicts = TimeConflictResolver.checkConflicts(
                request, lab, List.of(openHours), approvedBookings, courses);

        if (expectConflict) {
            assertFalse(conflicts.isEmpty(), caseName + " 预期有冲突");
            assertTrue(conflicts.contains("TIME_CONFLICT"), caseName + " 预期 TIME_CONFLICT");
        } else {
            assertTrue(conflicts.isEmpty(), caseName + " 预期无冲突，实际: " + conflicts);
        }
    }

    @Test
    void shouldReturnLabClosedWhenLabNotAvailable() {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setLabId(1L);
        request.setDate(LocalDate.now().toString());
        request.setStartTime("08:00");
        request.setEndTime("10:00");

        Lab lab = new Lab();
        lab.setStatus(LabStatus.CLOSED);

        List<String> conflicts = TimeConflictResolver.checkConflicts(
                request, lab, Collections.emptyList(), Collections.emptyList(), Collections.emptyList());

        assertTrue(conflicts.contains("LAB_CLOSED"));
        assertEquals(1, conflicts.size());
    }

    @Test
    void shouldReturnOutsideOpenHoursWhenNoSlotCovers() {
        BookingCreateRequest request = new BookingCreateRequest();
        request.setLabId(1L);
        request.setDate(LocalDate.now().toString());
        request.setStartTime("23:00");
        request.setEndTime("23:59");

        Lab lab = new Lab();
        lab.setStatus(LabStatus.AVAILABLE);

        LabHours openHours = new LabHours();
        openHours.setDayOfWeek(LocalDate.now().getDayOfWeek().getValue());
        openHours.setOpenTime("08:00");
        openHours.setCloseTime("18:00");

        List<String> conflicts = TimeConflictResolver.checkConflicts(
                request, lab, List.of(openHours), Collections.emptyList(), Collections.emptyList());

        assertTrue(conflicts.contains("OUTSIDE_OPEN_HOURS"));
    }
}
