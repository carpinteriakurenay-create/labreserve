package com.labreserve.controller;

import com.labreserve.support.BaseIntegrationTest;
import com.labreserve.support.annotations.WithMockStudent;
import com.labreserve.support.annotations.WithMockTeacher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class NoticeControllerTest extends BaseIntegrationTest {

    @Nested
    class ListNotices {

        @Test
        @WithMockStudent
        void shouldReturnNoticesList() throws Exception {
            mockMvc.perform(get("/api/notices"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @WithMockStudent
        void shouldFilterByType() throws Exception {
            mockMvc.perform(get("/api/notices").param("type", "GENERAL"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class CreateNotice {

        @Test
        @WithMockTeacher
        void shouldCreateNoticeSuccessfully() throws Exception {
            mockMvc.perform(post("/api/notices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title":"Test Notice","content":"This is a test notice content.","type":"GENERAL","priority":"NORMAL"}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @WithMockTeacher
        void shouldFailWithoutRequiredFields() throws Exception {
            mockMvc.perform(post("/api/notices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title":"","content":""}"""))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class DeleteNotice {

        @Test
        @WithMockTeacher
        void shouldDeleteNotice() throws Exception {
            // First create a notice
            var result = mockMvc.perform(post("/api/notices")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"title":"To Delete","content":"Content","type":"GENERAL","priority":"LOW"}"""))
                    .andReturn();

            mockMvc.perform(delete("/api/notices/1"))
                    .andExpect(status().isOk());
        }
    }
}
