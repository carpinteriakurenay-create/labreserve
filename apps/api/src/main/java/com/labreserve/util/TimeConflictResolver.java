package com.labreserve.util;

import com.labreserve.dto.BookingCreateRequest;
import com.labreserve.entity.Booking;
import com.labreserve.entity.Course;
import com.labreserve.entity.Lab;
import com.labreserve.entity.LabHours;
import com.labreserve.enums.LabStatus;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TimeConflictResolver {

    private TimeConflictResolver() {
    }

    public static boolean isTimeOverlap(String start1, String end1, String start2, String end2) {
        if (start1.compareTo(end1) > 0 || start2.compareTo(end2) > 0) {
            throw new IllegalArgumentException("跨天时间段不被允许");
        }
        return start1.compareTo(end2) < 0 && end1.compareTo(start2) > 0;
    }

    public static boolean isWithinOpenHours(String bookingStart, String bookingEnd, List<LabHours> openHours) {
        for (LabHours hours : openHours) {
            if (hours.getOpenTime().compareTo(bookingStart) <= 0
                    && hours.getCloseTime().compareTo(bookingEnd) >= 0) {
                return true;
            }
        }
        return false;
    }

    public static List<String> checkConflicts(
            BookingCreateRequest request,
            Lab lab,
            List<LabHours> openHours,
            List<Booking> approvedBookings,
            List<Course> courses) {

        List<String> conflicts = new ArrayList<>();

        if (lab.getStatus() != LabStatus.AVAILABLE) {
            conflicts.add("LAB_CLOSED");
            return conflicts;
        }

        LocalDate date = LocalDate.parse(request.getDate());
        DayOfWeek dayOfWeek = date.getDayOfWeek();
        int dayValue = dayOfWeek.getValue();

        List<LabHours> dayOpenHours = openHours.stream()
                .filter(h -> h.getDayOfWeek() == dayValue)
                .toList();

        if (!isWithinOpenHours(request.getStartTime(), request.getEndTime(), dayOpenHours)) {
            conflicts.add("OUTSIDE_OPEN_HOURS");
        }

        boolean hasTimeConflict = false;

        for (Booking approved : approvedBookings) {
            if (approved.getLabId().equals(request.getLabId())
                    && approved.getDate().equals(date)
                    && isTimeOverlap(request.getStartTime(), request.getEndTime(),
                            approved.getStartTime(), approved.getEndTime())) {
                hasTimeConflict = true;
                break;
            }
        }

        if (!hasTimeConflict) {
            for (Course course : courses) {
                if (course.getLabId().equals(request.getLabId())
                        && !date.isBefore(course.getStartDate())
                        && !date.isAfter(course.getEndDate())
                        && course.getDayOfWeek() == dayValue
                        && isTimeOverlap(request.getStartTime(), request.getEndTime(),
                                course.getStartTime(), course.getEndTime())) {
                    hasTimeConflict = true;
                    break;
                }
            }
        }

        if (hasTimeConflict) {
            conflicts.add("TIME_CONFLICT");
        }

        return conflicts;
    }
}
