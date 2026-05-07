package com.skillbridge.communication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.communication.service.UserDirectoryClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DiagnosticControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserDirectoryClient userDirectoryClient;

    @Test
    void instanceReturnsCurrentServiceMetadata() throws Exception {
        mockMvc.perform(get("/diagnostics/instance"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.service").value("communication-service"))
            .andExpect(jsonPath("$.data.port").exists())
            .andExpect(jsonPath("$.data.instanceId").exists());
    }

    @Test
    void userServiceInstanceReturnsLoadBalancedUserServiceDiagnostics() throws Exception {
        when(userDirectoryClient.getUserServiceInstance()).thenReturn(objectMapper.readTree("""
            {
              "success": true,
              "data": {
                "service": "user-service",
                "port": "3001",
                "instanceId": "user-service:3001"
              }
            }
            """));

        mockMvc.perform(get("/diagnostics/user-service-instance"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.communicationInstanceId").exists())
            .andExpect(jsonPath("$.data.userService.data.service").value("user-service"))
            .andExpect(jsonPath("$.data.userService.data.instanceId").value("user-service:3001"));
    }
}
