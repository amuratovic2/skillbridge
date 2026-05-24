package com.skillbridge.order.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.skillbridge.order.dto.CreateOrderRequest;
import com.skillbridge.order.dto.OrderStatusUpdateRequest;
import com.skillbridge.order.model.Order;
import com.skillbridge.order.model.OrderStatus;
import com.skillbridge.order.service.OrderPatchService;
import com.skillbridge.order.service.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(OrderController.class)
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private OrderPatchService orderPatchService;

    private Order fakeOrder() {
        Order o = new Order();
        o.setClientId(1);
        o.setGigId(2);
        o.setSellerId(5);
        o.setTotalCost(new BigDecimal("100"));
        o.setStatus(OrderStatus.PENDING);
        o.setMaxRevisions(3);
        return o;
    }

    @Test
    void createOrder_success() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();
        req.setGigId(2);

        when(orderService.create(anyInt(), anyInt(), any())).thenReturn(fakeOrder());

        mockMvc.perform(post("/orders")
                .header("x-user-id", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void createOrder_missingGigId_returns400() throws Exception {
        CreateOrderRequest req = new CreateOrderRequest();

        mockMvc.perform(post("/orders")
                .header("x-user-id", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void findById_success() throws Exception {
        when(orderService.findById(1L)).thenReturn(fakeOrder());

        mockMvc.perform(get("/orders/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void getMyBuyingOrders_returnsPaginatedResult() throws Exception {
        when(orderService.findByClient(anyInt(), anyInt(), anyInt(), anyString(), anyString()))
            .thenReturn(Map.of(
                "data", List.of(),
                "meta", Map.of("total", 0, "page", 1, "limit", 10, "totalPages", 0)
            ));

        mockMvc.perform(get("/orders/my/buying")
                .header("x-user-id", 1)
                .param("page", "1")
                .param("limit", "10"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void updateStatus_success() throws Exception {
        Order o = fakeOrder();
        o.setStatus(OrderStatus.ACCEPTED);
        when(orderService.updateStatus(anyLong(), anyInt(), anyString(), any(OrderStatus.class), any()))
            .thenReturn(o);

        OrderStatusUpdateRequest req = new OrderStatusUpdateRequest();
        req.setStatus("ACCEPTED");

        mockMvc.perform(patch("/orders/1/status")
                .header("x-user-id", 1)
                .header("x-user-role", "USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
            .andExpect(status().isOk());
    }

    @Test
    void updateStatus_invalidStatus_returns400() throws Exception {
        mockMvc.perform(patch("/orders/1/status")
                .header("x-user-id", 1)
                .header("x-user-role", "USER")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"status\": \"INVALID_STATUS\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void requestRevision_success() throws Exception {
        Order o = fakeOrder();
        o.setStatus(OrderStatus.REVISION_REQUESTED);
        when(orderService.requestRevision(anyLong(), anyInt(), any())).thenReturn(o);

        mockMvc.perform(post("/orders/1/revision")
                .header("x-user-id", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"message\": \"Molim izmijeni boje\"}"))
            .andExpect(status().isOk());
    }

    @Test
    void findOverdue_returnsEmptyList() throws Exception {
        when(orderService.findOverdue()).thenReturn(List.of());

        mockMvc.perform(get("/orders/overdue"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.data").isArray());
    }

    @Test
    void getStatusStatistics_returnsMap() throws Exception {
        when(orderService.getStatusStatistics()).thenReturn(Map.of("PENDING", 5L, "COMPLETED", 10L));

        mockMvc.perform(get("/orders/statistics/by-status"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void batchCreate_emptyList_returns400() throws Exception {
        mockMvc.perform(post("/orders/batch")
                .header("x-user-id", 1)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"orders\": []}"))
            .andExpect(status().isBadRequest());
    }
}
