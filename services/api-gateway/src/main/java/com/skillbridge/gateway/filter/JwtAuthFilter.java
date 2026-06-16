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
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.http.server.PathContainer;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.Set;
import java.net.URI;

@Component
public class JwtAuthFilter implements GlobalFilter, Ordered {

    private static final Set<String> IDENTITY_HEADERS = Set.of(
        "x-user-id",
        "x-user-role",
        "x-user-email",
        "x-authenticated-by",
        "x-internal-gateway-secret",
        "x-internal-gateway-id",
        "x-internal-gateway-timestamp",
        "x-internal-gateway-nonce",
        "x-internal-gateway-signature"
    );

    private static final String GATEWAY_ID_HEADER = "x-internal-gateway-id";
    private static final String TIMESTAMP_HEADER = "x-internal-gateway-timestamp";
    private static final String NONCE_HEADER = "x-internal-gateway-nonce";
    private static final String SIGNATURE_HEADER = "x-internal-gateway-signature";

    private static final Set<String> ALL_ROLES = Set.of("CLIENT", "FREELANCER", "ADMIN");
    private static final Set<String> FREELANCER_ROLES = Set.of("FREELANCER", "ADMIN");
    private static final Set<String> CLIENT_ROLES = Set.of("CLIENT", "ADMIN");
    private static final Set<String> ADMIN_ROLES = Set.of("ADMIN");

    private final WebClient webClient;
    private final String gatewayInternalSecret;
    private final String gatewayId;
    private final Optional<PrivateKey> signingKey;
    private final List<RouteRule> publicRules;
    private final List<RouteRule> roleRules;

    public JwtAuthFilter(
        @Value("${services.user-service-url}") String userServiceUrl,
        @Value("${gateway.internal-secret}") String gatewayInternalSecret,
        @Value("${gateway.signature.gateway-id:api-gateway}") String gatewayId,
        @Value("${gateway.signature.private-key:}") String signingPrivateKey
    ) {
        this.webClient = WebClient.builder().baseUrl(userServiceUrl).build();
        this.gatewayInternalSecret = gatewayInternalSecret;
        this.gatewayId = gatewayId;
        this.signingKey = loadPrivateKey(signingPrivateKey);
        PathPatternParser parser = new PathPatternParser();
        this.publicRules = List.of(
            new RouteRule(HttpMethod.GET, parser.parse("/"), ALL_ROLES),
            new RouteRule(HttpMethod.POST, parser.parse("/api/auth/register"), ALL_ROLES),
            new RouteRule(HttpMethod.POST, parser.parse("/api/auth/login"), ALL_ROLES),
            new RouteRule(HttpMethod.POST, parser.parse("/api/auth/refresh"), ALL_ROLES),
            new RouteRule(HttpMethod.POST, parser.parse("/api/auth/logout"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/actuator/health/**"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/gigs/**"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/categories/**"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/tags/**"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/users"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/users/{id:[0-9]+}"), ALL_ROLES),
            new RouteRule(HttpMethod.GET, parser.parse("/api/diagnostics/**"), ALL_ROLES),
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
            new RouteRule(HttpMethod.PATCH, parser.parse("/api/orders/{id:[0-9]+}"), ADMIN_ROLES),
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
            return chain.filter(addGatewaySecret(stripIdentityHeaders(exchange)));
        }

        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }

        String token = authHeader.substring(7);

        return webClient.post()
            .uri("/api/auth/validate")
            .headers(headers -> addInternalAuthHeaders(headers, HttpMethod.POST.name(), "/api/auth/validate"))
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

                    ServerWebExchange sanitized = stripIdentityHeaders(exchange);
                    ServerHttpRequest mutated = sanitized.getRequest().mutate()
                        .headers(headers -> {
                            headers.set("x-user-id", String.valueOf(data.get("userId")));
                            headers.set("x-user-role", role);
                            headers.set("x-user-email", String.valueOf(data.get("email")));
                            headers.set("x-authenticated-by", "api-gateway");
                            addInternalAuthHeaders(headers, method.name(), pathWithQuery(exchange.getRequest().getURI()));
                        })
                        .build();
                    return chain.filter(sanitized.mutate().request(mutated).build());
                }
                return reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid token");
            })
            .onErrorResume(ResponseStatusException.class,
                e -> reject(exchange, HttpStatus.UNAUTHORIZED, "Invalid token"))
            .onErrorResume(WebClientRequestException.class,
                e -> reject(exchange, HttpStatus.SERVICE_UNAVAILABLE, "user-service not available"))
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

    private ServerWebExchange addGatewaySecret(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
            .headers(headers -> addInternalAuthHeaders(
                headers,
                exchange.getRequest().getMethod().name(),
                pathWithQuery(exchange.getRequest().getURI())
            ))
            .build();
        return exchange.mutate().request(request).build();
    }

    private void addInternalAuthHeaders(HttpHeaders headers, String method, String path) {
        headers.set("x-internal-gateway-secret", gatewayInternalSecret);
        signingKey.ifPresent(privateKey -> {
            String timestamp = String.valueOf(Instant.now().getEpochSecond());
            String nonce = UUID.randomUUID().toString();
            headers.set(GATEWAY_ID_HEADER, gatewayId);
            headers.set(TIMESTAMP_HEADER, timestamp);
            headers.set(NONCE_HEADER, nonce);
            headers.set(SIGNATURE_HEADER, sign(privateKey, canonicalPayload(method, path, timestamp, nonce)));
        });
    }

    private String sign(PrivateKey privateKey, String payload) {
        try {
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initSign(privateKey);
            signature.update(payload.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(signature.sign());
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign internal gateway request", ex);
        }
    }

    private Optional<PrivateKey> loadPrivateKey(String configuredKey) {
        if (configuredKey == null || configuredKey.isBlank()) {
            return Optional.empty();
        }
        try {
            String normalized = configuredKey
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(normalized);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            return Optional.of(KeyFactory.getInstance("RSA").generatePrivate(keySpec));
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid gateway.signature.private-key", ex);
        }
    }

    private String canonicalPayload(String method, String path, String timestamp, String nonce) {
        return method + "\n" + path + "\n" + timestamp + "\n" + nonce;
    }

    private String pathWithQuery(URI uri) {
        String query = uri.getRawQuery();
        return query == null || query.isBlank() ? uri.getRawPath() : uri.getRawPath() + "?" + query;
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
