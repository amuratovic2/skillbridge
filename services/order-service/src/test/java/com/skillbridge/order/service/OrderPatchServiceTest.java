package com.skillbridge.order.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.skillbridge.order.model.Order;
import com.skillbridge.order.model.OrderStatus;
import com.skillbridge.order.repository.OrderRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderPatchServiceTest {

    @Mock private OrderRepository orderRepository;

    private OrderPatchService service;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        service = new OrderPatchService(orderRepository, objectMapper);
    }

    private Order existing() {
        Order o = new Order();
        o.setId(1L);
        o.setStatus(OrderStatus.ACCEPTED);
        o.setTotalCost(new BigDecimal("100.00"));
        o.setMaxRevisions(2);
        return o;
    }

    private JsonPatch patchFrom(String json) throws Exception {
        JsonNode node = objectMapper.readTree(json);
        return JsonPatch.fromJson(node);
    }

    @Test
    void patch_updatesAllowedFields() throws Exception {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing()));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Order result = service.patch(1L, 99, "ADMIN", patchFrom(
            "[{\"op\":\"replace\",\"path\":\"/totalCost\",\"value\":250.00}," +
            "{\"op\":\"replace\",\"path\":\"/maxRevisions\",\"value\":5}]"
        ));

        assertThat(result.getTotalCost()).isEqualByComparingTo("250.00");
        assertThat(result.getMaxRevisions()).isEqualTo(5);
    }

    @Test
    void patch_rejectsPatchOnStatus() throws Exception {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing()));

        JsonPatch patch = patchFrom(
            "[{\"op\":\"replace\",\"path\":\"/status\",\"value\":\"COMPLETED\"}]"
        );

        assertThatThrownBy(() -> service.patch(1L, 99, "ADMIN", patch))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void patch_rejectsNegativeTotalCost() throws Exception {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing()));

        JsonPatch patch = patchFrom(
            "[{\"op\":\"replace\",\"path\":\"/totalCost\",\"value\":-10}]"
        );

        assertThatThrownBy(() -> service.patch(1L, 99, "ADMIN", patch))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void patch_rejectsNegativeMaxRevisions() throws Exception {
        when(orderRepository.findById(1L)).thenReturn(Optional.of(existing()));

        JsonPatch patch = patchFrom(
            "[{\"op\":\"replace\",\"path\":\"/maxRevisions\",\"value\":-1}]"
        );

        assertThatThrownBy(() -> service.patch(1L, 99, "ADMIN", patch))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void patch_returns404WhenOrderMissing() throws Exception {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        JsonPatch patch = patchFrom(
            "[{\"op\":\"replace\",\"path\":\"/totalCost\",\"value\":200}]"
        );

        assertThatThrownBy(() -> service.patch(99L, 99, "ADMIN", patch))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void patch_rejectsNonAdmin() throws Exception {
        JsonPatch patch = patchFrom(
            "[{\"op\":\"replace\",\"path\":\"/totalCost\",\"value\":200}]"
        );

        assertThatThrownBy(() -> service.patch(1L, 1, "CLIENT", patch))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN));
    }
}
