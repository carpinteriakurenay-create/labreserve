package com.labreserve.controller;

import com.labreserve.support.BaseIntegrationTest;
import com.labreserve.support.annotations.WithMockAdmin;
import com.labreserve.support.annotations.WithMockStudent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class AuthControllerTest extends BaseIntegrationTest {

    @Nested
    class Register {

        @Test
        void shouldRegisterSuccessfully() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"newuser","password":"pass123","realName":"New User","email":"new@test.com"}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        void shouldFailWhenUsernameExists() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"student1","password":"pass123","realName":"Dup User"}"""))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("USERNAME_EXISTS"));
        }

        @Test
        void shouldFailWhenMissingRequiredFields() throws Exception {
            mockMvc.perform(post("/api/auth/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"","password":"","realName":""}"""))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    class Login {

        @Test
        void shouldLoginSuccessfully() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"student1","password":"password123"}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.token").exists())
                    .andExpect(jsonPath("$.data.user.role").value("STUDENT"));
        }

        @Test
        void shouldFailWithWrongPassword() throws Exception {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"student1","password":"wrongpassword"}"""))
                    .andExpect(status().is4xxClientError());
        }
    }

    @Nested
    class Me {

        @Test
        @WithMockStudent
        void shouldReturnCurrentUser() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.id").value(1));
        }

        @Test
        void shouldReturn401WhenNotAuthenticated() throws Exception {
            mockMvc.perform(get("/api/auth/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    class ChangePassword {

        @Test
        @WithMockStudent
        void shouldChangePasswordSuccessfully() throws Exception {
            mockMvc.perform(put("/api/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"oldPassword":"password123","newPassword":"newpass456"}"""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @WithMockStudent
        void shouldFailWithWrongOldPassword() throws Exception {
            mockMvc.perform(put("/api/auth/change-password")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"oldPassword":"wrongold","newPassword":"newpass456"}"""))
                    .andExpect(status().is4xxClientError())
                    .andExpect(jsonPath("$.code").value("WRONG_PASSWORD"));
        }
    }

    @Test
    void healthEndpointShouldBeAccessibleWithoutAuth() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk());
    }
}
