package com.labreserve.controller;

import com.labreserve.support.BaseIntegrationTest;
import com.labreserve.support.annotations.WithMockStudent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ValidationTest extends BaseIntegrationTest {

    @Nested
    class XssVectors {

        @Test
        @WithMockStudent
        void shouldHandleScriptTagInPurpose() throws Exception {
            mockMvc.perform(post("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":1,"date":"2026-07-10","startTime":"10:00","endTime":"12:00","purpose":"<script>alert('xss')</script>"}"""))
                    .andExpect(status().isCreated()); // accepts and stores, frontend responsible for escaping
        }

        @Test
        @WithMockStudent
        void shouldHandleImgTagInComment() throws Exception {
            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"bookingId":3,"rating":5,"comment":"<img src=x onerror=alert(1)>"}"""))
                    .andExpect(status().isCreated());
        }

        @Test
        @WithMockStudent
        void shouldHandleScriptTagInRepairLogDescription() throws Exception {
            mockMvc.perform(post("/api/repair-logs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"equipmentId":1,"description":"<script>alert(1)</script>"}"""))
                    .andExpect(status().isCreated());
        }
    }

    @Nested
    class SqlInjection {

        @Test
        @WithMockStudent
        void shouldHandleSqlInjectionInQueryParam() throws Exception {
            mockMvc.perform(get("/api/labs").param("name", "' OR '1'='1"))
                    .andExpect(status().isOk()); // returns empty results, no error
        }

        @Test
        @WithMockStudent
        void shouldHandleDropTableInBody() throws Exception {
            mockMvc.perform(post("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":1,"date":"2026-07-10","startTime":"10:00","endTime":"12:00","purpose":"test'; DROP TABLE bookings;--"}"""))
                    .andExpect(status().isCreated()); // treated as literal text
        }
    }

    @Nested
    class BoundaryValues {

        @Test
        @WithMockStudent
        void shouldRejectEmptyRequiredFields() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"","password":"","realName":""}"""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockStudent
        void shouldRejectRatingOutOfRange() throws Exception {
            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"bookingId":3,"rating":6,"comment":"Test"}"""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockStudent
        void shouldRejectNegativeRating() throws Exception {
            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"bookingId":3,"rating":0,"comment":"Test"}"""))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class EnumValidation {

        @Test
        @WithMockStudent
        void shouldRejectInvalidBookingStatus() throws Exception {
            mockMvc.perform(get("/api/bookings").param("status", "INVALID_STATUS"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @WithMockStudent
        void shouldRejectInvalidLabStatus() throws Exception {
            mockMvc.perform(get("/api/labs").param("status", "DELETED"))
                    .andExpect(status().is4xxClientError());
        }
    }
}
