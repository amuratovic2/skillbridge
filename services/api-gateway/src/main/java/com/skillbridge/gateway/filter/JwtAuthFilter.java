package com.skillbridge.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.http.server.PathContainer;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Set<String> IDENTITY_HEADERS = Set.of(
        "x-user-id",
        "x-user-role",
        "x-user-email",
        "x-authenticated-by"
    );

    private static final Set<String> ALL_ROLES = Set.of("CLIENT", "FREELANCER", "ADMIN");
    private static final Set<String> FREELANCER_ROLES = Set.of("FREELANCER", "ADMIN");
    private static final Set<String> CLIENT_ROLES = Set.of("CLIENT", "ADMIN");
    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN");

    private final WebClient webClient;
    private final List<RouteRule> publicRules;
    private final List<RouteRule> roleRules;

    public JwtAuthFilter(@Value("${services.user-service-url}") String userServiceUrl) {
        this.webClient = WebClient.builder().baseUrl(userServiceUrl).build();
        PathPatternParser parser = new PathPatternParser();
        this.publicRules = List.of(
            new RouteRule(null, parser.parse("/api/auth/**"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/actuator/health/**"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/gigs/**"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/categories/**"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/tags/**"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/users"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/users/{id:[0-9]+}"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/skills"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/skills/user/{userId:[0-9]+}"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/portfolios/user/{userId:[0-9]+}"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/reviews/user/{userId:[0-9]+}"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/reviews/order/{orderId:[0-9]+}"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/reviews/rating/{userId:[0-9]+}"), ALL_ROLES)
        );
        this.roleRules = List.of(
            new RouteRule(HttpMethod.POST, parser.parse("/api/gigs/**"), FREELANCER_ROLES),
            new RouteRule(HttpMethod.PATCH, parser.parse("/api/gigs/**"), FREELANCER_ROLES),
            new RouteRule(HttpMethod.DELETE, parser.parse("/api/gigs/**"), FREELANCER_ROLES),

            new RouteRule(HttpMethod.POST, parser.parse("/api/categories/**"), ADMIN_ROLES),
            new RouteRule(HttpMethod.PATCH, parser.parse("/api/categories/**"), ADMIN_ROLES),
            new RouteRule(HttpMethod.DELETE, parser.parse("/api/categories/**"), ADMIN_ROLES),
            new RouteRule(HttpMethod.POST, parser.parse("/api/skills"), ADMIN_ROLES),
            new RouteRule(HttpMethod.POST, parser.parse("/api/skills/batch"), ADMIN_ROLES),

            new RouteRule(HttpMethod.POST, parser.parse("/api/portfolios/**"), FREELANCER_ROLES),
            new RouteRule(HttpMethod.PATCH, parser.parse("/api/portfolios/**"), FREELANCER_ROLES),
            new RouteRule(HttpMethod.DELETE, parser.parse("/api/portfolios/**"), FREELANCER_ROLES),

            new RouteRule(HttpMethod.POST, parser.parse("/api/orders/**"), CLIENT_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/orders/overdue"), ADMIN_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/orders/statistics/**"), ADMIN_ROLES),
            new RouteRule(HttpMethod.POST, parser.parse("/api/deliveries/**"), FREELANCER_ROLES),

            new RouteRule(HttpMethod.POST, parser.parse("/api/custom-offers/**"), FREELANCER_ROLES),
            new RouteRule(HttpMethod.PATCH, parser.parse("/api/custom-offers/*/withdraw"), FREELANCER_ROLES),
            new RouteRule(HttpMethod.PATCH, parser.parse("/api/custom-offers/*/respond"), CLIENT_ROLES),

            new RouteRule(HttpMethod.GET, parser.parse("/api/disputes"), ADMIN_ROLES),
            new RouteRule(HttpMethod.PATCH, parser.parse("/api/disputes/*/assign"), ADMIN_ROLES),
            new RouteRule(HttpMethod.PATCH, parser.parse("/api/disputes/*/resolve"), ADMIN_ROLES)
        );
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        HttpMethod method = exchange.getRequest().getMethod();

        if (HttpMethod.OPTIONS.equals(method) || isPublic(method, path)) {
            return chain.filter(stripIdentityHeaders(exchange));
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Missing bearer token");
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
                    String role = String.valueOf(data.get("role"));
                    if (!isAllowed(method, path, role)) {
                        return reject(exchange, HttpStatus.FORBIDDEN, "Insufficient permissions");
                    }

                    ServerHttpRequest mutated = stripIdentityHeaders(exchange).getRequest().mutate()
                        .headers(headers -> {
                            headers.set("x-user-id", String.valueOf(data.get("userId")));
                            headers.set("x-user-role", role);
                            headers.set("x-user-email", String.valueOf(data.get("email")));
                            headers.set("x-authenticated-by", "api-gateway");
                        })
                        .build();
                    return chain.filter(exchange.mutate().request(mutated).build());
                }
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid token");
            })
            .onErrorResume(ResponseStatusException.class,
                e -> reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid token"))
            .onErrorResume(e -> reject(exchange, HttpStatus.UNAUTHORIZED, "Token validation failed"));
    }

    @Override
    public int getOrder() {
        return -1;
    }

    private boolean isPublic(HttpMethod method, String path) {
        return publicRules.stream().anyMatch(rule -> rule.matches(method, path));
    }

    private boolean isAllowed(HttpMethod method, String path, String role) {
        return roleRules.stream()
            .filter(rule -> rule.matches(method, path))
            .findFirst()
            .map(rule -> rule.allowedRoles().contains(role))
            .orElse(ALL_ROLES.contains(role));
    }

    private ServerWebExchange stripIdentityHeaders(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
            .headers(headers -> IDENTITY_HEADERS.forEach(headers::remove))
            .build();
        return exchange.mutate().request(request).build();
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status, String message) {
        var response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] bytes = ("""
            {"success":false,"error":"%s","message":"%s"}
            """.formatted(status.getReasonPhrase().toLowerCase().replace(" ", "_"), message))
            .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = response.bufferFactory().wrap(bytes);
        return response.writeWith(Mono.just(buffer));
    }

    private record RouteRule(HttpMethod method, PathPattern pattern, Set<String> allowedRoles) {
        boolean matches(HttpMethod requestMethod, String path) {
            return (method == null || method.equals(requestMethod))
                && pattern.matches(PathContainer.parsePath(path));
        }
    }
}
