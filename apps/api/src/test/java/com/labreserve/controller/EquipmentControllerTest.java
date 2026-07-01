package com.labreserve.controller;

import com.labreserve.support.BaseIntegrationTest;
import com.labreserve.support.annotations.WithMockAdmin;
import com.labreserve.support.annotations.WithMockStudent;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class EquipmentControllerTest extends BaseIntegrationTest {

    @Nested
    class ListEquipment {

        @Test
        @WithMockStudent
        void shouldReturnEquipmentList() throws Exception {
            mockMvc.perform(get("/api/equipment"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.records").isArray());
        }

        @Test
        @WithMockStudent
        void shouldFilterByLabId() throws Exception {
            mockMvc.perform(get("/api/equipment").param("labId", "1"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class CreateEquipment {

        @Test
        @WithMockAdmin
        void shouldCreateEquipmentSuccessfully() throws Exception {
            mockMvc.perform(post("/api/equipment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":1,"name":"New Device","model":"Model X","serialNumber":"SN-2026-002","description":"Test"}"""))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("SUCCESS"));
        }

        @Test
        @WithMockAdmin
        void shouldThrowWhenDuplicateSerialNumber() throws Exception {
            mockMvc.perform(post("/api/equipment")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"labId":1,"name":"Dup","serialNumber":"FPGA-2026-001"}"""))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("SERIAL_NUMBER_EXISTS"));
        }
    }

    @Nested
    class GetEquipment {

        @Test
        @WithMockStudent
        void shouldReturnEquipmentDetail() throws Exception {
            mockMvc.perform(get("/api/equipment/1"))
                    .andExpect(status().isOk());
        }
    }

    @Nested
    class UpdateStatus {

        @Test
        @WithMockAdmin
        void shouldUpdateEquipmentStatus() throws Exception {
            mockMvc.perform(put("/api/equipment/1/status")
                            .param("status", "MAINTENANCE"))
                    .andExpect(status().isOk());
        }
    }
}
