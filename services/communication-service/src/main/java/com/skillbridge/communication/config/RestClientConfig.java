package com.skillbridge.communication.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    @LoadBalanced
    public RestTemplate loadBalancedRestTemplate(
        RestTemplateBuilder builder,
        @Value("${communication.user-service.connect-timeout-ms:1000}") long connectTimeoutMs,
        @Value("${communication.user-service.read-timeout-ms:1500}") long readTimeoutMs,
        @Value("${gateway.internal-secret}") String gatewayInternalSecret
    ) {
        return builder
            .connectTimeout(Duration.ofMillis(connectTimeoutMs))
            .readTimeout(Duration.ofMillis(readTimeoutMs))
            .additionalInterceptors((request, body, execution) -> {
                request.getHeaders().set("x-internal-gateway-secret", gatewayInternalSecret);
                return execution.execute(request, body);
            })
            .build();
    }
}
