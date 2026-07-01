package com.labreserve.controller;

import com.labreserve.support.BaseIntegrationTest;
import com.labreserve.support.annotations.WithMockAdmin;
import com.labreserve.support.annotations.WithMockStudent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class LabControllerTest extends BaseIntegrationTest {

    @Nested
    class ListLabs {

        @Test
        @WithMockStudent
        void shouldReturnLabsList() throws Exception {
            mockMvc.perform(get("/api/labs"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test
        @WithMockStudent
        void shouldFilterByName() throws Exception {
            mockMvc.perform(get("/api/labs").param("name", "Computer"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class GetLabById {

        @Test
        @WithMockStudent
        void shouldReturnLabDetail() throws Exception {
            mockMvc.perform(get("/api/labs/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.name").value("Computer Lab A"));
        }

        @Test
        @WithMockStudent
        void shouldReturn404ForNonExistentLab() throws Exception {
            mockMvc.perform(get("/api/labs/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class CreateLab {

        @Test
        @WithMockAdmin
        void shouldCreateLabSuccessfully() throws Exception {
            mockMvc.perform(post("/api/labs")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"New Lab","location":"Building C","capacity":30,"description":"New lab"}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    @Nested
    class GetAndUpdateHours {

        @Test
        @WithMockStudent
        void shouldReturnLabHours() throws Exception {
            mockMvc.perform(get("/api/labs/1/hours"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @WithMockAdmin
        void shouldReplaceLabHours() throws Exception {
            mockMvc.perform(put("/api/labs/1/hours")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"hours":[{"dayOfWeek":1,"openTime":"09:00","closeTime":"17:00"}]}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }
}
