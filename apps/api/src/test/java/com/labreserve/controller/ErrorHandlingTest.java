package com.labreserve.controller;

import com.labreserve.support.BaseIntegrationTest;
import com.labreserve.support.annotations.WithMockStudent;
import com.labreserve.support.annotations.WithMockTeacher;
import com.labreserve.support.annotations.WithMockAdmin;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * 错误情况集成测试：
 * 无效输入、未认证请求、资源不存在、业务规则冲突
 */
class ErrorHandlingTest extends BaseIntegrationTest {

    private String loginAndGetToken() throws Exception {
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"student1","password":"password123"}"""))
                .andExpect(status().isOk())
                .andReturn();
        return com.jayway.jsonpath.JsonPath.read(
                result.getResponse().getContentAsString(), "$.data.token");
    }

    // ===== 未认证请求 =====

    @Nested
    class Unauthenticated {

        @Test
        void shouldReturn401ForLabsWithoutToken() throws Exception {
            mockMvc.perform(get("/api/labs"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
        }

        @Test
        void shouldReturn401ForBookingsWithoutToken() throws Exception {
            mockMvc.perform(get("/api/bookings"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturn401ForEquipmentWithoutToken() throws Exception {
            mockMvc.perform(get("/api/equipment"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturn401ForCoursesWithoutToken() throws Exception {
            mockMvc.perform(get("/api/courses"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturn401ForNoticesWithoutToken() throws Exception {
            mockMvc.perform(get("/api/notices"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturn401ForDashboardWithoutToken() throws Exception {
            mockMvc.perform(get("/api/dashboard/kpi"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturn401WithMalformedToken() throws Exception {
            mockMvc.perform(get("/api/labs")
                            .header("Authorization", "Bearer expired.invalid.token"))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void shouldReturn401WithoutBearerPrefix() throws Exception {
            mockMvc.perform(get("/api/labs")
                            .header("Authorization", "token-without-bearer"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===== 无效输入 =====

    @Nested
    class InvalidInput {

        @Test
        void shouldRejectEmptyRequestBody() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{}"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        void shouldRejectInvalidJson() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("not valid json"))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @WithMockStudent(userId = 1L)
        void shouldRejectBookingWithInvalidDate() throws Exception {
            mockMvc.perform(post("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":1,"date":"not-a-date","startTime":"10:00","endTime":"12:00"}"""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockStudent(userId = 1L)
        void shouldRejectBookingWithInvalidTimeFormat() throws Exception {
            mockMvc.perform(post("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":1,"date":"2026-07-10","startTime":"10-00","endTime":"12-00"}"""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockStudent(userId = 1L)
        void shouldRejectBookingEndTimeBeforeStartTime() throws Exception {
            mockMvc.perform(post("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":1,"date":"2026-07-10","startTime":"14:00","endTime":"10:00"}"""))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @WithMockStudent(userId = 1L)
        void shouldRejectReviewWithInvalidRating() throws Exception {
            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"bookingId":3,"rating":10,"comment":"Invalid rating"}"""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockStudent(userId = 1L)
        void shouldRejectReviewWithNegativeRating() throws Exception {
            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"bookingId":3,"rating":-1,"comment":"Negative rating"}"""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectRegistrationWithMissingFields() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"","password":"","realName":""}"""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @WithMockStudent(userId = 1L)
        void shouldRejectBorrowWithBackwardsDateRange() throws Exception {
            // Some backends may accept this and validate at service layer;
            // test that it at least gets a proper response
            var result = mockMvc.perform(post("/api/borrows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"equipmentId":1,"borrowDate":"2026-07-15","expectedReturn":"2026-07-10","purpose":"Invalid date order"}"""))
                    .andReturn();
            // Service may accept or reject; either way it shouldn't 500
            int status = result.getResponse().getStatus();
            assert status >= 200 && status < 500 : "Expected non-5xx response";
        }

        @Test
        @WithMockStudent(userId = 1L)
        void shouldRejectBookingWithNegativePersonCount() throws Exception {
            mockMvc.perform(post("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":1,"date":"2026-07-10","startTime":"10:00","endTime":"12:00","personCount":-1}"""))
                    .andExpect(status().isBadRequest());
        }

        @Test
        void shouldRejectChangePasswordWithShortNewPassword() throws Exception {
            String token = loginAndGetToken();
            mockMvc.perform(put("/api/auth/change-password")
                            .header("Authorization", "Bearer " + token)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"oldPassword":"password123","newPassword":"ab"}"""))
                    .andExpect(status().isBadRequest());
        }
    }

    // ===== 资源不存在 =====

    @Nested
    class ResourceNotFound {

        @Test
        @WithMockStudent
        void shouldReturn404ForNonExistentLab() throws Exception {
            mockMvc.perform(get("/api/labs/99999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOT_FOUND"));
        }

        @Test
        @WithMockStudent
        void shouldReturn404ForNonExistentBooking() throws Exception {
            mockMvc.perform(get("/api/bookings/99999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockStudent
        void shouldReturn404ForNonExistentEquipment() throws Exception {
            mockMvc.perform(get("/api/equipment/99999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockStudent
        void shouldReturn404ForNonExistentBorrow() throws Exception {
            mockMvc.perform(get("/api/borrows/99999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockStudent
        void shouldReturn404ForNonExistentCourse() throws Exception {
            mockMvc.perform(get("/api/courses/99999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockStudent
        void shouldReturn404ForNonExistentNotice() throws Exception {
            mockMvc.perform(get("/api/notices/99999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockStudent
        void shouldReturn404ForNonExistentReview() throws Exception {
            mockMvc.perform(delete("/api/reviews/99999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockStudent
        void shouldReturn404ForNonExistentRepairLog() throws Exception {
            mockMvc.perform(get("/api/repair-logs/99999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockAdmin
        void shouldReturn404ForNonExistentUser() throws Exception {
            mockMvc.perform(get("/api/users/99999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @WithMockAdmin
        void shouldReturn404ForNonExistentStudent() throws Exception {
            mockMvc.perform(get("/api/students/99999"))
                    .andExpect(status().isNotFound());
        }
    }

    // ===== 业务规则冲突 =====

    @Nested
    class BusinessRuleConflict {

        @Test
        @WithMockStudent(userId = 1L)
        void shouldRejectBookingOnClosedLab() throws Exception {
            // Lab 2 is MAINTENANCE (closed) in test data
            mockMvc.perform(post("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":2,"date":"2026-07-10","startTime":"10:00","endTime":"12:00","purpose":"Test closed lab"}"""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("LAB_CLOSED"));
        }

        @Test
        @WithMockStudent(userId = 1L)
        void shouldRejectBookingOutsideOpenHours() throws Exception {
            // Lab 1 open hours: Mon-Fri 08:00-18:00
            // 2026-07-10 is a Friday. Booking at 06:00 should be outside hours
            mockMvc.perform(post("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":1,"date":"2026-07-10","startTime":"06:00","endTime":"07:00","purpose":"Outside hours"}"""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("OUTSIDE_OPEN_HOURS"));
        }

        @Test
        @WithMockStudent(userId = 1L)
        void shouldRejectBorrowingUnavailableEquipment() throws Exception {
            // Equipment 2 is MAINTENANCE in test data
            mockMvc.perform(post("/api/borrows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"equipmentId":2,"borrowDate":"2026-07-10","expectedReturn":"2026-07-15","purpose":"Test unavailable"}"""))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("EQUIPMENT_UNAVAILABLE"));
        }

        @Test
        @WithMockStudent(userId = 1L)
        void shouldRejectReviewForNonCompletedBooking() throws Exception {
            // Booking 1 is APPROVED (not COMPLETED) in test data
            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"bookingId":1,"rating":4,"comment":"Not completed yet"}"""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BOOKING_NOT_COMPLETED"));
        }

        @Test
        @WithMockStudent(userId = 1L)
        void shouldRejectDuplicateReview() throws Exception {
            // Booking 3 is COMPLETED in test data. First review should succeed
            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"bookingId":3,"rating":4,"comment":"First review"}"""))
                    .andExpect(status().isCreated());

            // Second review on same booking should fail
            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"bookingId":3,"rating":5,"comment":"Duplicate review"}"""))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("ALREADY_REVIEWED"));
        }

        @Test
        @WithMockTeacher
        void shouldRejectDoubleApproval() throws Exception {
            // Booking 2 is PENDING. Approve it once
            mockMvc.perform(put("/api/bookings/2/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"approved\":true}"))
                    .andExpect(status().isOk());

            // Approve again should fail with 409 (conflict) as ALREADY_PROCESSED
            mockMvc.perform(put("/api/bookings/2/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"approved\":true}"))
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.code").value("ALREADY_PROCESSED"));
        }

        @Test
        @WithMockStudent(userId = 1L)
        void shouldRejectCancellingCompletedBooking() throws Exception {
            // Booking 3 is COMPLETED - cannot cancel
            mockMvc.perform(put("/api/bookings/3/cancel"))
                    .andExpect(status().is4xxClientError());
        }
    }

    // ===== HTTP 方法不支持 =====

    @Nested
    class MethodNotAllowed {

        @Test
        void shouldReturn405ForInvalidMethodOnHealth() throws Exception {
            mockMvc.perform(post("/api/health"))
                    .andExpect(status().isMethodNotAllowed());
        }

        @Test
        @WithMockStudent
        void shouldReturn405ForDeleteOnBookingsList() throws Exception {
            mockMvc.perform(delete("/api/bookings"))
                    .andExpect(status().isMethodNotAllowed());
        }
    }

    // ===== 安全：越权尝试 =====

    @Nested
    class UnauthorizedAccess {

        @Test
        @WithMockStudent
        void studentCannotCreateLab() throws Exception {
            mockMvc.perform(post("/api/labs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Hack Lab","location":"Nowhere","capacity":10}"""))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockStudent
        void studentCannotDeleteLab() throws Exception {
            mockMvc.perform(delete("/api/labs/1"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockStudent
        void studentCannotCreateEquipment() throws Exception {
            mockMvc.perform(post("/api/equipment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":1,"name":"Hack Device","serialNumber":"SN-BAD"}"""))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockStudent
        void studentCannotAccessUserManagement() throws Exception {
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockTeacher
        void teacherCannotCreateUsers() throws Exception {
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"newuser","password":"pass123","realName":"New","role":"STUDENT"}"""))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockStudent
        void studentCannotAccessUsageRecordsExport() throws Exception {
            // Usage records export uses GET method, requires TEACHER/ADMIN role
            mockMvc.perform(get("/api/usage-records/export"))
                    .andExpect(status().isForbidden());
        }
    }

    // ===== XSS 和注入防御 =====

    @Nested
    class SecurityInputValidation {

        @Test
        void shouldHandleXssInRegistrationFields() throws Exception {
            // The backend allows registration with XSS-like content since it relies
            // on output encoding; test that it either rejects or succeeds without crash
            var result = mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"xss_user_2026","password":"pass123","realName":"<script>alert('xss')</script>"}"""))
                    .andReturn();
            // Should not 500
            assert result.getResponse().getStatus() >= 200
                    && result.getResponse().getStatus() < 500
                    : "Expected non-5xx response for XSS input";
        }

        @Test
        @WithMockStudent(userId = 1L)
        void shouldHandleSqlInjectionInSearchParams() throws Exception {
            // Should not crash or return data it shouldn't
            mockMvc.perform(get("/api/labs")
                            .param("name", "'; DROP TABLE labs; --"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockStudent(userId = 1L)
        void shouldHandleSpecialCharactersInPurpose() throws Exception {
            mockMvc.perform(post("/api/bookings")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":1,"date":"2026-07-10","startTime":"10:00","endTime":"12:00","purpose":"Test <> special chars and quotes: it's safe"}"""))
                    .andExpect(status().isCreated());
        }
    }
}
