package com.labreserve.controller;

import com.labreserve.support.BaseIntegrationTest;
import com.labreserve.support.annotations.WithMockAdmin;
import com.labreserve.support.annotations.WithMockStudent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class RepairLogControllerTest extends BaseIntegrationTest {

    @Nested
    class ListRepairLogs {

        @Test
        @WithMockStudent
        void shouldReturnRepairLogsList() throws Exception {
            mockMvc.perform(get("/api/repair-logs"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class CreateRepairLog {

        @Test
        @WithMockStudent
        void shouldCreateRepairLogSuccessfully() throws Exception {
            mockMvc.perform(post("/api/repair-logs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"equipmentId":1,"description":"Screen is flickering"}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    @Nested
    class UpdateStatus {

        @Test
        @WithMockAdmin
        void shouldUpdateRepairLogStatus() throws Exception {
            var result = mockMvc.perform(post("/api/repair-logs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"equipmentId":1,"description":"Needs repair"}"""))
                    .andExpect(status().isCreated())
                    .andReturn();
            String createdId = com.jayway.jsonpath.JsonPath.read(
                    result.getResponse().getContentAsString(), "$.data.id").toString();

            mockMvc.perform(put("/api/repair-logs/" + createdId + "/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"status":"IN_PROGRESS"}"""))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockStudent
        void studentCannotUpdateStatus() throws Exception {
            mockMvc.perform(put("/api/repair-logs/1/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"status":"IN_PROGRESS"}"""))
                    .andExpect(status().isForbidden());
        }
    }
}
