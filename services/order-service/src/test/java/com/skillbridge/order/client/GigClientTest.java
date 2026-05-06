package com.skillbridge.order.client;

import com.skillbridge.order.dto.ApiResponse;
import com.skillbridge.order.dto.GigDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for GigClient (Mockito only, no Spring context).
 *
 * NOTE: @CircuitBreaker is a Spring AOP proxy — it has no effect here because
 * GigClient is instantiated directly by Mockito. These tests cover only the
 * raw HTTP-layer behaviour of GigClient.getGig().
 * Circuit-breaker behaviour (OPEN/HALF-OPEN/fallback) is tested separately
 * via integration tests with a full Spring context.
 */
@ExtendWith(MockitoExtension.class)
class GigClientTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GigClient gigClient;

    private GigDto buildGig(Integer id, String status) {
        GigDto gig = new GigDto();
        gig.setId(id);
        gig.setFreelancerId(5);
        gig.setCost(new BigDecimal("99.99"));
        gig.setDeliveryTime(7);
        gig.setRevisionCount(3);
        gig.setStatus(status);
        return gig;
    }

    @Test
    void getGig_returnsGigDto_onSuccessfulResponse() {
        ApiResponse<GigDto> apiResponse = ApiResponse.ok(buildGig(1, "ACTIVE"));

        when(restTemplate.exchange(
            eq(GigClient.GIG_SERVICE_BASE_URL + 1),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(apiResponse));

        GigDto result = gigClient.getGig(1);

        assertThat(result.getId()).isEqualTo(1);
        assertThat(result.getFreelancerId()).isEqualTo(5);
        assertThat(result.getCost()).isEqualByComparingTo("99.99");
        assertThat(result.getDeliveryTime()).isEqualTo(7);
        assertThat(result.getRevisionCount()).isEqualTo(3);
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
    }

    @Test
    void getGig_usesServiceNameUrl_notHardcodedIp() {
        // Verifies the URL starts with the Eureka service name — Eureka/LoadBalancer
        // resolves this at runtime. If this ever changes to an IP, load balancing breaks.
        ApiResponse<GigDto> apiResponse = ApiResponse.ok(buildGig(42, "ACTIVE"));

        when(restTemplate.exchange(
            eq("http://gig-service/api/gigs/42"),
            eq(HttpMethod.GET),
            isNull(),
            any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(apiResponse));

        GigDto result = gigClient.getGig(42);
        assertThat(result).isNotNull();
    }

    @Test
    void getGig_throws404_whenGigNotFound() {
        when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)
        )).thenThrow(HttpClientErrorException.create(
            HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        assertThatThrownBy(() -> gigClient.getGig(999))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void getGig_throws502_onOtherHttpError() {
        when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)
        )).thenThrow(HttpClientErrorException.create(
            HttpStatus.FORBIDDEN, "Forbidden", null, null, null));

        assertThatThrownBy(() -> gigClient.getGig(1))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.BAD_GATEWAY));
    }

    @Test
    void getGig_propagatesResourceAccessException_soCircuitBreakerCanCountIt() {
        // ResourceAccessException (connection refused / timeout) is intentionally NOT
        // caught inside getGig() — Resilience4j sees it and counts it as a failure.
        // In a real Spring context the @CircuitBreaker fallback would return 503.
        when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)
        )).thenThrow(new ResourceAccessException("Connection refused"));

        assertThatThrownBy(() -> gigClient.getGig(1))
            .isInstanceOf(ResourceAccessException.class);
    }

    @Test
    void getGig_throws404_whenResponseBodyIsNull() {
        when(restTemplate.exchange(
            anyString(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class)
        )).thenReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> gigClient.getGig(1))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex ->
                assertThat(((ResponseStatusException) ex).getStatusCode())
                    .isEqualTo(HttpStatus.NOT_FOUND));
    }
}
