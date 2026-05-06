package com.skillbridge.order.client;

import com.skillbridge.order.config.RestTemplateConfig;
import com.skillbridge.order.dto.ApiResponse;
import com.skillbridge.order.dto.GigDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Method;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Verifies load-balancing contract without starting a full Spring context.
 *
 * What we can assert without a live cluster:
 *  1. The base URL uses the Eureka service name — never a hardcoded IP/port.
 *  2. RestTemplateConfig.restTemplate() carries @LoadBalanced so Spring Cloud
 *     will intercept calls and resolve the service name via Eureka at runtime.
 *  3. Every getGig() call targets the service-name URL, regardless of gigId.
 *  4. The gigId is correctly appended to the base URL.
 */
@ExtendWith(MockitoExtension.class)
class GigClientLoadBalancingTest {

    @Mock
    private RestTemplate restTemplate;

    private GigClient gigClient;

    @BeforeEach
    void setUp() {
        gigClient = new GigClient(restTemplate);
    }

    @Test
    void gigServiceBaseUrl_usesEurekaServiceName_notHardcodedIp() {
        assertThat(GigClient.GIG_SERVICE_BASE_URL)
            .startsWith("http://gig-service/")
            .doesNotContain("localhost")
            .doesNotContain("127.0.0.1")
            .doesNotContainPattern(":\\d{4,5}/");
    }

    @Test
    void restTemplateConfig_restTemplateBean_isAnnotatedWithLoadBalanced() throws NoSuchMethodException {
        Method restTemplateMethod = RestTemplateConfig.class.getMethod("restTemplate");
        assertThat(restTemplateMethod.isAnnotationPresent(LoadBalanced.class))
            .as("RestTemplateConfig.restTemplate() must have @LoadBalanced so Spring Cloud " +
                "intercepts calls and resolves 'gig-service' via Eureka")
            .isTrue();
    }

    @Test
    @SuppressWarnings("unchecked")
    void getGig_alwaysCallsServiceNameUrl_notHardcodedHost() {
        ApiResponse<GigDto> apiResponse = buildSuccessResponse();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(),
            any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(apiResponse));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

        gigClient.getGig(1);
        gigClient.getGig(2);
        gigClient.getGig(3);

        verify(restTemplate, times(3)).exchange(
            urlCaptor.capture(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class));

        urlCaptor.getAllValues().forEach(url ->
            assertThat(url)
                .startsWith("http://gig-service/")
                .doesNotContain("localhost")
                .doesNotContain("127.0.0.1")
        );
    }

    @Test
    @SuppressWarnings("unchecked")
    void getGig_appendsGigIdToBaseUrl() {
        ApiResponse<GigDto> apiResponse = buildSuccessResponse();
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), isNull(),
            any(ParameterizedTypeReference.class)))
            .thenReturn(ResponseEntity.ok(apiResponse));

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

        gigClient.getGig(42);

        verify(restTemplate).exchange(
            urlCaptor.capture(), eq(HttpMethod.GET), isNull(), any(ParameterizedTypeReference.class));

        assertThat(urlCaptor.getValue()).isEqualTo("http://gig-service/api/gigs/42");
    }

    private ApiResponse<GigDto> buildSuccessResponse() {
        GigDto gig = new GigDto();
        gig.setId(1);
        gig.setFreelancerId(5);
        gig.setCost(new BigDecimal("100.00"));
        gig.setDeliveryTime(7);
        gig.setRevisionCount(3);
        gig.setStatus("ACTIVE");
        return ApiResponse.ok(gig);
    }
}
