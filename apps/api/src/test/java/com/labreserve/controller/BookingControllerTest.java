package com.labreserve.controller;

import com.labreserve.support.BaseIntegrationTest;
import com.labreserve.support.annotations.WithMockStudent;
import com.labreserve.support.annotations.WithMockTeacher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class BookingControllerTest extends BaseIntegrationTest {

    @Nested
    class ListBookings {

        @Test
        @WithMockStudent
        void shouldReturnBookingsList() throws Exception {
            mockMvc.perform(get("/api/bookings"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @WithMockStudent
        void shouldFilterByStatus() throws Exception {
            mockMvc.perform(get("/api/bookings").param("status", "APPROVED"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class CreateBooking {

        @Test
        @WithMockStudent(userId = 1L)
        void shouldCreateBookingSuccessfully() throws Exception {
            mockMvc.perform(post("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":1,"date":"2026-07-10","startTime":"10:00","endTime":"12:00","purpose":"Test"}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @WithMockStudent
        void shouldThrowWhenLabNotFound() throws Exception {
            mockMvc.perform(post("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":999,"date":"2026-07-10","startTime":"10:00","endTime":"12:00","purpose":"Test"}"""))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        @WithMockStudent
        void shouldThrowWhenMissingLabId() throws Exception {
            mockMvc.perform(post("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"date":"2026-07-10","startTime":"10:00","endTime":"12:00"}"""))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class GetAvailableSlots {

        @Test
        @WithMockStudent
        void shouldReturnAvailableSlots() throws Exception {
            mockMvc.perform(get("/api/bookings/available-slots")
                            .param("labId", "1")
                            .param("date", "2026-07-10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    class MyBookings {

        @Test
        @WithMockStudent
        void shouldReturnMyBookings() throws Exception {
            mockMvc.perform(get("/api/bookings/mine"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    @Nested
    class PendingApprovals {

        @Test
        @WithMockTeacher
        void teacherCanSeePendingApprovals() throws Exception {
            mockMvc.perform(get("/api/bookings/pending"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class ApproveBooking {

        @Test
        @WithMockTeacher(userId = 2L)
        void shouldApproveBooking() throws Exception {
            mockMvc.perform(put("/api/bookings/2/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"approved":true}"""))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockTeacher
        void shouldRejectBooking() throws Exception {
            mockMvc.perform(put("/api/bookings/2/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"approved":false,"rejectReason":"Time conflict"}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    @Nested
    class CancelBooking {

        @Test
        @WithMockStudent(userId = 1L)
        void shouldCancelOwnPendingBooking() throws Exception {
            mockMvc.perform(put("/api/bookings/2/cancel"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @WithMockStudent(userId = 2L)
        void shouldThrowWhenCancellingOthersBooking() throws Exception {
            mockMvc.perform(put("/api/bookings/1/cancel"))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    @Nested
    class CompleteBooking {

        @Test
        @WithMockTeacher
        void shouldCompleteApprovedBooking() throws Exception {
            mockMvc.perform(put("/api/bookings/1/complete"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @WithMockTeacher
        void shouldThrowWhenCompletingPendingBooking() throws Exception {
            mockMvc.perform(put("/api/bookings/2/complete"))
                    .andExpect(status().is4xxClientError());
        }
    }
}
