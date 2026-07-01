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

class BorrowControllerTest extends BaseIntegrationTest {

    @Nested
    class ListBorrows {

        @Test
        @WithMockTeacher
        void shouldReturnBorrowsList() throws Exception {
            mockMvc.perform(get("/api/borrows"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    @Nested
    class CreateBorrow {

        @Test
        @WithMockStudent
        void shouldCreateBorrowSuccessfully() throws Exception {
            mockMvc.perform(post("/api/borrows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"equipmentId":1,"borrowDate":"2026-07-10","expectedReturn":"2026-07-15","purpose":"Testing"}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @WithMockStudent
        void shouldThrowWhenEquipmentUnavailable() throws Exception {
            mockMvc.perform(post("/api/borrows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"equipmentId":2,"borrowDate":"2026-07-10","expectedReturn":"2026-07-15","purpose":"Testing"}"""))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("EQUIPMENT_UNAVAILABLE"));
        }

        @Test
        @WithMockStudent
        void shouldThrowWhenEquipmentNotFound() throws Exception {
            mockMvc.perform(post("/api/borrows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"equipmentId":999,"borrowDate":"2026-07-10","expectedReturn":"2026-07-15","purpose":"Testing"}"""))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class MyBorrows {

        @Test
        @WithMockStudent
        void shouldReturnMyBorrows() throws Exception {
            mockMvc.perform(get("/api/borrows/mine"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    @Nested
    class ApproveBorrow {

        @Test
        @WithMockTeacher
        void shouldApproveBorrow() throws Exception {
            var result = mockMvc.perform(post("/api/borrows")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"equipmentId":1,"borrowDate":"2026-07-10","expectedReturn":"2026-07-15","purpose":"Testing"}"""))
                    .andExpect(status().isCreated())
                    .andReturn();
            String createdId = com.jayway.jsonpath.JsonPath.read(
                    result.getResponse().getContentAsString(), "$.data.id").toString();

            mockMvc.perform(put("/api/borrows/" + createdId + "/approve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"approved":true}"""))
                    .andExpect(status().isOk());
        }
    }
}
