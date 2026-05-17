package com.skillbridge.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.cloud.gateway.route.Route;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.net.URI;
import java.nio.charset.StandardCharsets;

import static org.springframework.cloud.gateway.support.ServerWebExchangeUtils.GATEWAY_ROUTE_ATTR;

@Component
public class ServiceAvailabilityFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange)
            .onErrorResume(ex -> {
                String serviceName = resolveServiceName(exchange);
                if (serviceName == null || exchange.getResponse().isCommitted()) {
                    return Mono.error(ex);
                }
                return reject(exchange, serviceName + " not available");
            });
    }

    @Override
    public int getOrder() {
        return -2;
    }

    private String resolveServiceName(ServerWebExchange exchange) {
        Route route = exchange.getAttribute(GATEWAY_ROUTE_ATTR);
        if (route == null) {
            return null;
        }

        URI uri = route.getUri();
        if ("lb".equalsIgnoreCase(uri.getScheme()) && uri.getHost() != null) {
            return uri.getHost();
        }

        String routeId = route.getId();
        if ("auth-service".equals(routeId) || "user-service-users".equals(routeId)) {
            return "user-service";
        }
        if ("communication-diagnostics".equals(routeId)) {
            return "communication-service";
        }
        return routeId;
    }

    private Mono<Void> reject(ServerWebExchange exchange, String message) {
        var response = exchange.getResponse();
        response.setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = ("""
            {"success":false,"error":"service_unavailable","message":"%s"}
            """.formatted(message))
            .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }
}
