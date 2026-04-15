package com.skillbridge.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private final WebClient webClient;

    public JwtAuthFilter(@Value("${services.user-service-url}") String userServiceUrl) {
        this.webClient = WebClient.builder().baseUrl(userServiceUrl).build();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (path.startsWith("/api/auth")) {
            return chain.filter(exchange);
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return chain.filter(exchange);
        }

        String token = authHeader.substring(7);

        return webClient.post()
            .uri("/api/auth/validate")
            .bodyValue(Map.of("token", token))
            .retrieve()
            .bodyToMono(Map.class)
            .flatMap(response -> {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = (Map<String, Object>) response.get("data");
                if (data != null) {
                    ServerHttpRequest mutated = exchange.getRequest().mutate()
                        .header("x-user-id", String.valueOf(data.get("userId")))
                        .header("x-user-role", String.valueOf(data.get("role")))
                        .header("x-user-email", String.valueOf(data.get("email")))
                        .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                }
                return chain.filter(exchange);
            })
            .onErrorResume(e -> chain.filter(exchange));
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
