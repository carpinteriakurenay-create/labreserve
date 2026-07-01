package com.labreserve.controller;

import com.labreserve.support.BaseIntegrationTest;
import com.labreserve.support.annotations.WithMockStudent;
import com.labreserve.support.annotations.WithMockTeacher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class CourseControllerTest extends BaseIntegrationTest {

    @Nested
    class ListCourses {

        @Test
        @WithMockStudent
        void shouldReturnCoursesList() throws Exception {
            mockMvc.perform(get("/api/courses"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    @Nested
    class CreateCourse {

        @Test
        @WithMockTeacher
        void shouldCreateCourseSuccessfully() throws Exception {
            mockMvc.perform(post("/api/courses")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"name":"Advanced CS","labId":1,"teacherId":2,"semester":"2025-2026-2","dayOfWeek":4,"startTime":"10:00","endTime":"12:00","startDate":"2026-07-01","endDate":"2026-12-31","className":"CS2201"}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    @Nested
    class MyCourses {

        @Test
        @WithMockStudent
        void shouldReturnStudentScheduleWhenClassNameProvided() throws Exception {
            mockMvc.perform(get("/api/courses/mine").param("className", "CS2101"))
                    .andExpect(status().isOk());
        }
    }
}
