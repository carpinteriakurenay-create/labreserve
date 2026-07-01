package com.labreserve.controller;

import com.labreserve.support.BaseIntegrationTest;
import com.labreserve.support.annotations.WithMockAdmin;
import com.labreserve.support.annotations.WithMockStudent;
import com.labreserve.support.annotations.WithMockTeacher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StudentControllerTest extends BaseIntegrationTest {

    @Nested
    class ListStudents {

        @Test
        @WithMockStudent
        void shouldReturnStudentsList() throws Exception {
            mockMvc.perform(get("/api/students"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @WithMockStudent
        void shouldFilterByLabId() throws Exception {
            mockMvc.perform(get("/api/students").param("labId", "1"))
                    .andExpect(status().isOk());
        }

        @Test
        @WithMockStudent
        void shouldFilterByName() throws Exception {
            mockMvc.perform(get("/api/students").param("name", "Test"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class CreateStudent {

        @Test
        @WithMockTeacher
        void shouldCreateStudentSuccessfully() throws Exception {
            mockMvc.perform(post("/api/students")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":1,"name":"Zhang San","gender":"MALE","age":20,"address":"CS Department"}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @WithMockTeacher
        void shouldFailWithoutName() throws Exception {
            mockMvc.perform(post("/api/students")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":1,"name":"","gender":"MALE","age":20}"""))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class DeleteStudent {

        @Test
        @WithMockAdmin
        void shouldDeleteStudent() throws Exception {
            // Create a student first
            var result = mockMvc.perform(post("/api/students")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":1,"name":"To Delete","gender":"MALE","age":20,"address":"Test"}"""))
                    .andExpect(status().isCreated())
                    .andReturn();
            String createdId = com.jayway.jsonpath.JsonPath.read(
                    result.getResponse().getContentAsString(), "$.data.id").toString();

            mockMvc.perform(delete("/api/students/" + createdId))
                    .andExpect(status().isOk());
        }
    }
}
