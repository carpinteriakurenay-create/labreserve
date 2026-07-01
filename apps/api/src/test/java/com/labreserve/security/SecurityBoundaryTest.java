package com.labreserve.security;

import com.labreserve.support.BaseIntegrationTest;
import com.labreserve.support.annotations.WithMockAdmin;
import com.labreserve.support.annotations.WithMockStudent;
import com.labreserve.support.annotations.WithMockTeacher;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class SecurityBoundaryTest extends BaseIntegrationTest {

    // ---- Authentication: 401 tests ----

    @Test
    void shouldReturn401WithoutToken() throws Exception {
        mockMvc.perform(get("/api/labs"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WithInvalidToken() throws Exception {
        mockMvc.perform(get("/api/labs")
                        .header("Authorization", "Bearer eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.invalidsignature"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn401WithMissingBearerPrefix() throws Exception {
        mockMvc.perform(get("/api/labs")
                        .header("Authorization", "token-without-bearer"))
                .andExpect(status().isUnauthorized());
    }

    // ---- Authorization: Student privilege boundaries ----

    @Test
    @WithMockStudent
    void studentCanAccessLabsList() throws Exception {
        mockMvc.perform(get("/api/labs"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockStudent
    void studentCannotAccessAdminLabsCreate() throws Exception {
        mockMvc.perform(post("/api/labs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Hack Lab","location":"Bad","capacity":10,"description":"Should fail"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockStudent
    void studentCannotAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockStudent
    void studentCannotAccessAdminEquipmentCreate() throws Exception {
        mockMvc.perform(post("/api/equipment")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"labId":1,"name":"Hack Device","serialNumber":"SN-HACK"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockStudent
    void studentCannotAccessPendingApprovals() throws Exception {
        mockMvc.perform(get("/api/bookings/pending"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockStudent
    void studentCannotAccessUsageRecords() throws Exception {
        mockMvc.perform(get("/api/usage-records"))
                .andExpect(status().isForbidden());
    }

    // ---- Authorization: Teacher privilege boundaries ----

    @Test
    @WithMockTeacher
    void teacherCanAccessPendingApprovals() throws Exception {
        mockMvc.perform(get("/api/bookings/pending"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockTeacher
    void teacherCanAccessUsageRecords() throws Exception {
        mockMvc.perform(get("/api/usage-records"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockTeacher
    void teacherCannotAccessAdminUsers() throws Exception {
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockTeacher
    void teacherCannotAccessAdminLabsCreate() throws Exception {
        mockMvc.perform(post("/api/labs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Hack Lab","location":"Bad","capacity":10,"description":"Should fail"}"""))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockTeacher
    void teacherCannotDeleteStudent() throws Exception {
        mockMvc.perform(delete("/api/students/1"))
                .andExpect(status().isForbidden());
    }

    // ---- Authorization: Admin boundaries ----

    @Test
    @WithMockAdmin
    void adminCanAccessAllResources() throws Exception {
        mockMvc.perform(get("/api/labs")).andExpect(status().isOk());
        mockMvc.perform(get("/api/users")).andExpect(status().isOk());
        mockMvc.perform(get("/api/equipment")).andExpect(status().isOk());
        mockMvc.perform(get("/api/bookings/pending")).andExpect(status().isOk());
        mockMvc.perform(get("/api/usage-records")).andExpect(status().isOk());
    }

    // ---- Dashboard authorization ----

    @Test
    @WithMockStudent
    void studentCanAccessDashboardKpi() throws Exception {
        mockMvc.perform(get("/api/dashboard/kpi"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockStudent
    void studentCannotAccessStudentRanking() throws Exception {
        mockMvc.perform(get("/api/dashboard/student-ranking"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockTeacher
    void teacherCanAccessStudentRanking() throws Exception {
        mockMvc.perform(get("/api/dashboard/student-ranking"))
                .andExpect(status().isOk());
    }
}
