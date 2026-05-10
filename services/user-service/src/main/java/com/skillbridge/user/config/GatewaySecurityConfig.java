package com.skillbridge.user.config;

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

@Configuration
public class GatewaySecurityConfig implements WebMvcConfigurer {

    private static final String INTERNAL_HEADER = "x-internal-gateway-secret";

    private final boolean enabled;
    private final String internalSecret;

    public GatewaySecurityConfig(
        @Value("${gateway.security.enabled:true}") boolean enabled,
        @Value("${gateway.internal-secret}") String internalSecret
    ) {
        this.enabled = enabled;
        this.internalSecret = internalSecret;
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
            if (matchesSecret(providedSecret)) {
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
}
