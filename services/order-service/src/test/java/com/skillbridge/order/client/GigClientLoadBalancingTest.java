package com.skillbridge.order.client;

import com.skillbridge.order.dto.GigDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.DefaultServiceInstance;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.mock.http.client.MockClientHttpResponse;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.client.RestTemplate;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that the @LoadBalanced RestTemplate distributes requests across
 * multiple gig-service instances (round-robin).
 *
 * How it works:
 *  1. @LoadBalancerClient configures a static list of two fake instances.
 *  2. A recording interceptor is placed AFTER the LoadBalancerInterceptor in
 *     the chain, so it receives requests with the already-resolved host:port.
 *  3. The interceptor short-circuits the HTTP call and returns a canned JSON
 *     response, so no real network is needed.
 *  4. After N calls we verify that both instances were hit roughly equally.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
@LoadBalancerClient(name = "gig-service", configuration = GigClientLoadBalancingTest.StaticInstancesConfig.class)
class GigClientLoadBalancingTest {

    @Autowired
    private GigClient gigClient;

    @Autowired
    private RestTemplate restTemplate;

    private final List<String> hitHosts = new CopyOnWriteArrayList<>();
    private ClientHttpRequestInterceptor recorder;

    @BeforeEach
    void addRecordingInterceptor() {
        hitHosts.clear();
        // This interceptor sits AFTER the LB interceptor, so it sees the resolved host.
        // It also returns a mock response so no real TCP connection is made.
        recorder = (request, body, execution) -> {
            hitHosts.add(request.getURI().getHost() + ":" + request.getURI().getPort());
            byte[] json = ("{\"success\":true,\"data\":{\"id\":1,\"freelancerId\":5," +
                "\"cost\":100.00,\"deliveryTime\":7,\"revisionCount\":3,\"status\":\"ACTIVE\"}}").getBytes();
            ClientHttpResponse response = new MockClientHttpResponse(json, HttpStatus.OK);
            return response;
        };
        restTemplate.getInterceptors().add(recorder);
    }

    @AfterEach
    void removeRecordingInterceptor() {
        restTemplate.getInterceptors().remove(recorder);
    }

    @Test
    void roundRobin_distributesRequestsAcrossBothInstances() {
        for (int i = 0; i < 6; i++) {
            GigDto gig = gigClient.getGig(1);
            assertThat(gig).isNotNull();
        }

        assertThat(hitHosts).hasSize(6);
        assertThat(hitHosts).contains("instance1.local:8081", "instance2.local:8082");

        long hits1 = hitHosts.stream().filter(h -> h.contains("instance1")).count();
        long hits2 = hitHosts.stream().filter(h -> h.contains("instance2")).count();
        assertThat(hits1).isEqualTo(3);
        assertThat(hits2).isEqualTo(3);
    }

    @Test
    void allRequests_useResolvedHostNotServiceName() {
        for (int i = 0; i < 4; i++) {
            gigClient.getGig(1);
        }

        // None of the resolved URLs should still contain the Eureka service name
        assertThat(hitHosts).noneMatch(h -> h.contains("gig-service"));
        // All should be actual host:port pairs
        assertThat(hitHosts).allMatch(h -> h.matches(".+:\\d+"));
    }

    // -------------------------------------------------------------------------
    // Static load balancer config — two fake instances, no Eureka needed
    // -------------------------------------------------------------------------
    @Configuration
    static class StaticInstancesConfig {

        @Bean
        ServiceInstanceListSupplier staticSupplier() {
            return new ServiceInstanceListSupplier() {
                @Override
                public String getServiceId() {
                    return "gig-service";
                }

                @Override
                public Flux<List<ServiceInstance>> get() {
                    return Flux.just(List.of(
                        new DefaultServiceInstance("inst-1", "gig-service", "instance1.local", 8081, false),
                        new DefaultServiceInstance("inst-2", "gig-service", "instance2.local", 8082, false)
                    ));
                }
            };
        }
    }
}
