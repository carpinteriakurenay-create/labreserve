package com.labreserve.controller;

import com.labreserve.support.BaseIntegrationTest;
import com.labreserve.support.annotations.WithMockStudent;
import com.labreserve.support.annotations.WithMockTeacher;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class ReviewControllerTest extends BaseIntegrationTest {

    @Nested
    class ListReviews {

        @Test
        @WithMockStudent
        void shouldReturnReviewsList() throws Exception {
            mockMvc.perform(get("/api/reviews"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @WithMockStudent
        void shouldFilterByLabId() throws Exception {
            mockMvc.perform(get("/api/reviews").param("labId", "1"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class CreateReview {

        @Test
        @WithMockStudent(userId = 1L)
        void shouldCreateReviewForCompletedBooking() throws Exception {
            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"bookingId":3,"rating":5,"comment":"Excellent lab!"}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @WithMockStudent(userId = 1L)
        void shouldThrowWhenBookingNotCompleted() throws Exception {
            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"bookingId":1,"rating":5,"comment":"Not done yet"}"""))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("BOOKING_NOT_COMPLETED"));
        }

        @Test
        @WithMockStudent(userId = 2L)
        void shouldThrowWhenNotOwnBooking() throws Exception {
            mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"bookingId":3,"rating":5,"comment":"Not mine"}"""))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
        }
    }

    @Nested
    class DeleteReview {

        @Test
        @WithMockStudent(userId = 1L)
        void shouldDeleteOwnReview() throws Exception {
            // Create a review first
            var result = mockMvc.perform(post("/api/reviews")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"bookingId":3,"rating":4,"comment":"Good"}"""))
                    .andExpect(status().isCreated())
                    .andReturn();
            String createdId = com.jayway.jsonpath.JsonPath.read(
                    result.getResponse().getContentAsString(), "$.data.id").toString();

            mockMvc.perform(delete("/api/reviews/" + createdId))
                    .andExpect(status().isOk());
        }
    }
}
