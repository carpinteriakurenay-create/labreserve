package com.labreserve.controller;

import com.labreserve.support.BaseIntegrationTest;
import com.labreserve.support.annotations.WithMockAdmin;
import com.labreserve.support.annotations.WithMockStudent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class UserControllerTest extends BaseIntegrationTest {

    @Nested
    class ListUsers {

        @Test
        @WithMockAdmin
        void shouldReturnUsersList() throws Exception {
            mockMvc.perform(get("/api/users"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"))
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test
        @WithMockAdmin
        void shouldFilterByRole() throws Exception {
            mockMvc.perform(get("/api/users").param("role", "STUDENT"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class CreateUser {

        @Test
        @WithMockAdmin
        void shouldCreateUserSuccessfully() throws Exception {
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"newadmin","password":"pass123","realName":"New Admin","role":"ADMIN"}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @WithMockAdmin
        void shouldFailWithDuplicateUsername() throws Exception {
            mockMvc.perform(post("/api/users")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"username":"student1","password":"pass123","realName":"Dup"}"""))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("USERNAME_EXISTS"));
        }
    }

    @Nested
    class GetUser {

        @Test
        @WithMockAdmin
        void shouldReturnUserDetail() throws Exception {
            mockMvc.perform(get("/api/users/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1));
        }

        @Test
        @WithMockAdmin
        void shouldReturn404ForNonExistentUser() throws Exception {
            mockMvc.perform(get("/api/users/999"))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    class ToggleEnabled {

        @Test
        @WithMockAdmin
        void shouldToggleUserEnabled() throws Exception {
            mockMvc.perform(put("/api/users/1/toggle-enabled"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }
    }

    @Nested
    class DeleteUser {

        @Test
        @WithMockAdmin
        void shouldSoftDeleteUser() throws Exception {
            mockMvc.perform(delete("/api/users/1"))
                    .andExpect(status().isOk());
        }
    }
}
