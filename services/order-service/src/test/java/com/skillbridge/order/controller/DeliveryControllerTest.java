package com.skillbridge.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.order.dto.CreateDeliveryRequest;
import com.skillbridge.order.model.Delivery;
import com.skillbridge.order.model.Order;
import com.skillbridge.order.model.OrderStatus;
import com.skillbridge.order.service.DeliveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DeliveryController.class)
@ActiveProfiles("test")
class DeliveryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean DeliveryService deliveryService;

    private Delivery fakeDelivery() {
        Order o = new Order();
        o.setClientId(1);
        o.setGigId(2);
        o.setTotalCost(new BigDecimal("100"));
        o.setStatus(OrderStatus.IN_PROGRESS);

        Delivery d = new Delivery();
        d.setOrder(o);
        d.setVersionNumber(1);
        d.setMessage("Isporuka je gotova");
        return d;
    }

    @Test
    void createDelivery_success() throws Exception {
        CreateDeliveryRequest req = new CreateDeliveryRequest();
        req.setMessage("Isporuka gotova");

        when(deliveryService.create(anyLong(), anyInt(), any(), any(), any()))
            .thenReturn(fakeDelivery());

        mockMvc.perform(post("/deliveries/order/1")
                .header("x-user-id", 5)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createDelivery_missingMessage_returns400() throws Exception {
        CreateDeliveryRequest req = new CreateDeliveryRequest();

        mockMvc.perform(post("/deliveries/order/1")
                .header("x-user-id", 5)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void findByOrderId_returnsDeliveries() throws Exception {
        when(deliveryService.findByOrderId(1L)).thenReturn(List.of(fakeDelivery()));

        mockMvc.perform(get("/deliveries/order/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void findByVersion_notFound_returns404() throws Exception {
        when(deliveryService.findByVersion(1L, 99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/deliveries/order/1/version/99"))
            .andExpect(status().isNotFound());
    }

    @Test
    void findByVersion_success() throws Exception {
        when(deliveryService.findByVersion(1L, 1)).thenReturn(Optional.of(fakeDelivery()));

        mockMvc.perform(get("/deliveries/order/1/version/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }
}