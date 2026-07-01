package com.labreserve.controller;

import com.labreserve.support.BaseIntegrationTest;
import com.labreserve.support.annotations.WithMockStudent;
import com.labreserve.support.annotations.WithMockTeacher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class DashboardControllerTest extends BaseIntegrationTest {

    @Nested
    class Kpi {

        @Test
        @WithMockStudent
        void studentCanGetKpi() throws Exception {
            mockMvc.perform(get("/api/dashboard/kpi"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.todayBookings").isNumber())
                    .andExpect(jsonPath("$.data.todayBorrows").isNumber())
                    .andExpect(jsonPath("$.data.labUsageRate").isNumber())
                    .andExpect(jsonPath("$.data.pendingApprovals").isNumber());
        }

        @Test
        @WithMockTeacher
        void teacherCanGetKpi() throws Exception {
            mockMvc.perform(get("/api/dashboard/kpi"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class LabUsage {

        @Test
        @WithMockStudent
        void shouldReturnLabUsageStats() throws Exception {
            mockMvc.perform(get("/api/dashboard/lab-usage"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @WithMockStudent
        void shouldFilterByDateRange() throws Exception {
            mockMvc.perform(get("/api/dashboard/lab-usage")
                            .param("dateFrom", "2026-01-01")
                            .param("dateTo", "2026-12-31"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class EquipmentUsage {

        @Test
        @WithMockStudent
        void shouldReturnEquipmentUsageStats() throws Exception {
            mockMvc.perform(get("/api/dashboard/equipment-usage"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    class StudentRanking {

        @Test
        @WithMockTeacher
        void shouldReturnStudentRanking() throws Exception {
            mockMvc.perform(get("/api/dashboard/student-ranking"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @WithMockTeacher
        void shouldRespectLimitParam() throws Exception {
            mockMvc.perform(get("/api/dashboard/student-ranking").param("limit", "3"))
                    .andExpect(status().isOk());
        }
    }
}
