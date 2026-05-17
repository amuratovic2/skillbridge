package com.skillbridge.order.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Configuration
public class GatewaySecurityConfig implements WebMvcConfigurer {

    private static final String INTERNAL_HEADER = "x-internal-gateway-secret";
    private static final String GATEWAY_ID_HEADER = "x-internal-gateway-id";
    private static final String TIMESTAMP_HEADER = "x-internal-gateway-timestamp";
    private static final String NONCE_HEADER = "x-internal-gateway-nonce";
    private static final String SIGNATURE_HEADER = "x-internal-gateway-signature";
    private static final long MAX_SIGNATURE_AGE_SECONDS = 300;

    private final boolean enabled;
    private final boolean allowSharedSecret;
    private final String internalSecret;
    private final String expectedGatewayId;
    private final Optional<PublicKey> gatewayPublicKey;
    private final Map<String, Long> seenNonces = new ConcurrentHashMap<>();

    public GatewaySecurityConfig(
        @Value("${gateway.security.enabled:true}") boolean enabled,
        @Value("${gateway.security.allow-shared-secret:true}") boolean allowSharedSecret,
        @Value("${gateway.internal-secret}") String internalSecret,
        @Value("${gateway.signature.gateway-id:api-gateway}") String expectedGatewayId,
        @Value("${gateway.signature.public-key:}") String gatewayPublicKey
    ) {
        this.enabled = enabled;
        this.allowSharedSecret = allowSharedSecret;
        this.internalSecret = internalSecret;
        this.expectedGatewayId = expectedGatewayId;
        this.gatewayPublicKey = loadPublicKey(gatewayPublicKey);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        if (!enabled) {
            return;
        }
        registry.addInterceptor(new GatewayHeaderInterceptor())
            .addPathPatterns("/**")
            .excludePathPatterns("/actuator/**");
    }

    private class GatewayHeaderInterceptor implements HandlerInterceptor {
        @Override
        public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
            String providedSecret = request.getHeader(INTERNAL_HEADER);
            if (matchesGatewaySignature(request) || (allowSharedSecret && matchesSecret(providedSecret))) {
                return true;
            }

            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("""
                {"success":false,"error":"forbidden","message":"Requests must pass through the API gateway"}
                """);
            return false;
        }
    }

    private boolean matchesSecret(String providedSecret) {
        if (providedSecret == null || internalSecret == null || internalSecret.isBlank()) {
            return false;
        }
        return MessageDigest.isEqual(
            providedSecret.getBytes(StandardCharsets.UTF_8),
            internalSecret.getBytes(StandardCharsets.UTF_8)
        );
    }

    private boolean matchesGatewaySignature(HttpServletRequest request) {
        if (gatewayPublicKey.isEmpty()) {
            return false;
        }

        String gatewayId = request.getHeader(GATEWAY_ID_HEADER);
        String timestamp = request.getHeader(TIMESTAMP_HEADER);
        String nonce = request.getHeader(NONCE_HEADER);
        String providedSignature = request.getHeader(SIGNATURE_HEADER);
        if (!expectedGatewayId.equals(gatewayId) || isBlank(timestamp) || isBlank(nonce) || isBlank(providedSignature)) {
            return false;
        }

        long timestampSeconds;
        try {
            timestampSeconds = Long.parseLong(timestamp);
        } catch (NumberFormatException ex) {
            return false;
        }

        long now = Instant.now().getEpochSecond();
        if (Math.abs(now - timestampSeconds) > MAX_SIGNATURE_AGE_SECONDS || isReplay(gatewayId, nonce, timestampSeconds, now)) {
            return false;
        }

        try {
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(gatewayPublicKey.get());
            verifier.update(canonicalPayload(request, timestamp, nonce).getBytes(StandardCharsets.UTF_8));
            return verifier.verify(Base64.getDecoder().decode(providedSignature));
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean isReplay(String gatewayId, String nonce, long timestampSeconds, long now) {
        seenNonces.entrySet().removeIf(entry -> now - entry.getValue() > MAX_SIGNATURE_AGE_SECONDS);
        return seenNonces.putIfAbsent(gatewayId + ":" + nonce, timestampSeconds) != null;
    }

    private Optional<PublicKey> loadPublicKey(String configuredKey) {
        if (configuredKey == null || configuredKey.isBlank()) {
            return Optional.empty();
        }
        try {
            String normalized = configuredKey
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replaceAll("\\s", "");
            byte[] keyBytes = Base64.getDecoder().decode(normalized);
            return Optional.of(KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(keyBytes)));
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid gateway.signature.public-key", ex);
        }
    }

    private String canonicalPayload(HttpServletRequest request, String timestamp, String nonce) {
        String query = request.getQueryString();
        String path = query == null || query.isBlank() ? request.getRequestURI() : request.getRequestURI() + "?" + query;
        return request.getMethod() + "\n" + path + "\n" + timestamp + "\n" + nonce;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
