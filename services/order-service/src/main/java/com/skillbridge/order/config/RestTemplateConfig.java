package com.skillbridge.order.config;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.core5.util.Timeout;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {

    @Value("${rest.client.connect-timeout-ms:3000}")
    private int connectTimeoutMs;

    @Value("${rest.client.read-timeout-ms:5000}")
    private int readTimeoutMs;

    @Value("${rest.client.max-connections:50}")
    private int maxConnections;

    @Value("${rest.client.max-connections-per-route:20}")
    private int maxConnectionsPerRoute;

    /**
     * Load-balanced RestTemplate backed by Apache HttpClient 5.
     *
     * Why connection pooling matters: SimpleClientHttpRequestFactory opens a new
     * TCP connection on every call. Under load (e.g. batch orders) this exhausts
     * ephemeral ports. A pool reuses connections across requests.
     *
     * Why @LoadBalanced matters: Spring Cloud replaces "gig-service" with an actual
     * host:port from Eureka — no IP is hardcoded anywhere in the application.
     */
    @Bean
    @LoadBalanced
    public RestTemplate restTemplate() {
        PoolingHttpClientConnectionManager cm = new PoolingHttpClientConnectionManager();
        cm.setMaxTotal(maxConnections);
        cm.setDefaultMaxPerRoute(maxConnectionsPerRoute);

        RequestConfig requestConfig = RequestConfig.custom()
            .setConnectTimeout(Timeout.ofMilliseconds(connectTimeoutMs))
            .setResponseTimeout(Timeout.ofMilliseconds(readTimeoutMs))
            .build();

        CloseableHttpClient httpClient = HttpClients.custom()
            .setConnectionManager(cm)
            .setDefaultRequestConfig(requestConfig)
            .build();

        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }
}
