package com.skillbridge.order.client;

import com.skillbridge.order.dto.ApiResponse;
import com.skillbridge.order.dto.GigDto;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

/**
 * Synchronous HTTP client for gig-service.
 *
 * Service discovery: the RestTemplate is @LoadBalanced — Spring Cloud resolves
 * "gig-service" to an actual host:port via Eureka. No IP or port is hardcoded here.
 *
 * Resilience: a circuit breaker (Resilience4j) wraps each call.
 *   - CLOSED  → normal operation, calls go through.
 *   - OPEN    → after enough failures, fail-fast without network call → 503 response.
 *   - HALF-OPEN → after wait period, one probe call is allowed through.
 *
 * What counts as a failure:
 *   - ResourceAccessException  (connection refused, read timeout)
 *   - HttpServerErrorException (5xx from gig-service)
 * What does NOT open the circuit:
 *   - HttpClientErrorException.NotFound (404 — gig simply doesn't exist, valid response)
 */
@Service
public class GigClient {

    static final String GIG_SERVICE_BASE_URL = "http://gig-service/api/gigs/";

    private final RestTemplate restTemplate;

    public GigClient(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // Annotation order matters: @Retry wraps the method first, then @CircuitBreaker
    // wraps @Retry. Sequence on failure: retry up to 3 times → if still failing,
    // the circuit breaker counts ONE failure (not three) and eventually opens.
    @Retry(name = "gigService")
    @CircuitBreaker(name = "gigService", fallbackMethod = "getGigFallback")
    public GigDto getGig(Integer gigId) {
        try {
            ResponseEntity<ApiResponse<GigDto>> response = restTemplate.exchange(
                GIG_SERVICE_BASE_URL + gigId,
                HttpMethod.GET,
                null,
                new ParameterizedTypeReference<ApiResponse<GigDto>>() {}
            );
            ApiResponse<GigDto> body = response.getBody();
            if (body == null || !body.isSuccess() || body.getData() == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                    "Gig with id " + gigId + " not found");
            }
            return body.getData();
        } catch (HttpClientErrorException.NotFound e) {
            // 404 is a valid business response — gig simply does not exist.
            // Do NOT let this propagate as a "failure" to the circuit breaker.
            throw new ResponseStatusException(HttpStatus.NOT_FOUND,
                "Gig with id " + gigId + " not found");
        } catch (HttpClientErrorException e) {
            // Other 4xx errors (400, 403 …) — also valid responses, not service failures.
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Unexpected response from gig service: " + e.getStatusCode());
        }
        // ResourceAccessException (timeout, connection refused) and
        // HttpServerErrorException (5xx) are NOT caught here so Resilience4j
        // counts them as failures and can open the circuit.
    }

    /**
     * Called automatically by Resilience4j when the circuit is OPEN.
     * Returns 503 immediately, without making any network call, so the
     * rest of the system keeps running even while gig-service is down.
     */
    @SuppressWarnings("unused")
    private GigDto getGigFallback(Integer gigId, Throwable cause) {
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
            "Gig service is temporarily unavailable. Please try again later.");
    }
}
