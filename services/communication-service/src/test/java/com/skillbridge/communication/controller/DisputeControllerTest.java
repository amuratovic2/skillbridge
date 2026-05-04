package com.skillbridge.communication.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.communication.model.Dispute;
import com.skillbridge.communication.model.DisputeStatus;
import com.skillbridge.communication.repository.DisputeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DisputeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DisputeRepository disputeRepository;

    @BeforeEach
    void setUp() {
        disputeRepository.deleteAll();
    }

    @Test
    void createStoresValidDispute() throws Exception {
        Map<String, Object> body = Map.of(
            "orderId", 40,
            "reason", "Kasna isporuka",
            "description", "Isporuka kasni dva dana"
        );

        mockMvc.perform(post("/disputes")
                .header("x-user-id", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id", notNullValue()))
            .andExpect(jsonPath("$.data.status").value("OPEN"))
            .andExpect(jsonPath("$.data.initiatorId").value(1));
    }

    @Test
    void createRejectsDuplicateActiveDisputeForOrder() throws Exception {
        createDispute(40, 1, DisputeStatus.OPEN, null);
        Map<String, Object> body = Map.of(
            "orderId", 40,
            "reason", "Drugi razlog",
            "description", "Drugi opis"
        );

        mockMvc.perform(post("/disputes")
                .header("x-user-id", 2)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("conflict"));
    }

    @Test
    void createRejectsMissingReason() throws Exception {
        Map<String, Object> body = Map.of(
            "orderId", 40,
            "description", "Opis bez razloga"
        );

        mockMvc.perform(post("/disputes")
                .header("x-user-id", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("validation"))
            .andExpect(jsonPath("$.message", containsString("reason")));
    }

    @Test
    void findAllReturnsPagedAndFilteredDisputes() throws Exception {
        createDispute(40, 1, DisputeStatus.OPEN, null);
        createDispute(41, 2, DisputeStatus.UNDER_REVIEW, 99);

        mockMvc.perform(get("/disputes")
                .param("status", "OPEN")
                .param("page", "1")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.length()").value(1))
            .andExpect(jsonPath("$.data[0].status").value("OPEN"))
            .andExpect(jsonPath("$.meta.total").value(1));
    }

    @Test
    void findByIdReturnsDispute() throws Exception {
        Dispute dispute = createDispute(40, 1, DisputeStatus.OPEN, null);

        mockMvc.perform(get("/disputes/{id}", dispute.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.id").value(dispute.getId()));
    }

    @Test
    void findByIdReturnsNotFoundForMissingDispute() throws Exception {
        mockMvc.perform(get("/disputes/999"))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("not_found"));
    }

    @Test
    void assignMovesOpenDisputeUnderReview() throws Exception {
        Dispute dispute = createDispute(40, 1, DisputeStatus.OPEN, null);

        mockMvc.perform(patch("/disputes/{id}/assign", dispute.getId())
                .header("x-user-id", 99))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.adminId").value(99))
            .andExpect(jsonPath("$.data.status").value("UNDER_REVIEW"));
    }

    @Test
    void assignRejectsNonOpenDispute() throws Exception {
        Dispute dispute = createDispute(40, 1, DisputeStatus.UNDER_REVIEW, 99);

        mockMvc.perform(patch("/disputes/{id}/assign", dispute.getId())
                .header("x-user-id", 99))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("conflict"));
    }

    @Test
    void resolveClosesAssignedDispute() throws Exception {
        Dispute dispute = createDispute(40, 1, DisputeStatus.UNDER_REVIEW, 99);
        Map<String, Object> body = Map.of(
            "resolution", "Refund kupcu",
            "status", "RESOLVED_BUYER"
        );

        mockMvc.perform(patch("/disputes/{id}/resolve", dispute.getId())
                .header("x-user-id", 99)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true))
            .andExpect(jsonPath("$.data.status").value("RESOLVED_BUYER"))
            .andExpect(jsonPath("$.data.resolution").value("Refund kupcu"));
    }

    @Test
    void resolveRejectsDifferentAdmin() throws Exception {
        Dispute dispute = createDispute(40, 1, DisputeStatus.UNDER_REVIEW, 99);
        Map<String, Object> body = Map.of(
            "resolution", "Pokusaj zatvaranja",
            "status", "CLOSED"
        );

        mockMvc.perform(patch("/disputes/{id}/resolve", dispute.getId())
                .header("x-user-id", 100)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.success").value(false))
            .andExpect(jsonPath("$.error").value("conflict"));
    }

    private Dispute createDispute(Integer orderId, Integer initiatorId, DisputeStatus status, Integer adminId) {
        Dispute dispute = new Dispute();
        dispute.setOrderId(orderId);
        dispute.setInitiatorId(initiatorId);
        dispute.setReason("Test razlog");
        dispute.setDescription("Test opis");
        dispute.setStatus(status);
        dispute.setAdminId(adminId);
        dispute.setCreatedAt(LocalDateTime.now());
        return disputeRepository.save(dispute);
    }
}
