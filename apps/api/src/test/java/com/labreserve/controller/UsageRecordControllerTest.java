package com.labreserve.controller;

import com.labreserve.support.BaseIntegrationTest;
import com.labreserve.support.annotations.WithMockStudent;
import com.labreserve.support.annotations.WithMockTeacher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UsageRecordControllerTest extends BaseIntegrationTest {

    @Nested
    class ListUsageRecords {

        @Test
        @WithMockTeacher
        void shouldReturnUsageRecords() throws Exception {
            mockMvc.perform(get("/api/usage-records"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test
        @WithMockTeacher
        void shouldFilterByDateRange() throws Exception {
            mockMvc.perform(get("/api/usage-records")
                            .param("dateFrom", "2026-01-01")
                            .param("dateTo", "2026-12-31"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class ExportCsv {

        @Test
        @WithMockTeacher
        void shouldExportCsv() throws Exception {
            mockMvc.perform(get("/api/usage-records/export"))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type",
                            org.hamcrest.Matchers.containsString("text/csv")));
        }

        @Test
        @WithMockTeacher
        void shouldExportCsvWithBom() throws Exception {
            byte[] content = mockMvc.perform(get("/api/usage-records/export"))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsByteArray();

            // Verify UTF-8 BOM (0xEF, 0xBB, 0xBF)
            org.junit.jupiter.api.Assertions.assertEquals((byte) 0xEF, content[0]);
            org.junit.jupiter.api.Assertions.assertEquals((byte) 0xBB, content[1]);
            org.junit.jupiter.api.Assertions.assertEquals((byte) 0xBF, content[2]);
        }
    }
}
