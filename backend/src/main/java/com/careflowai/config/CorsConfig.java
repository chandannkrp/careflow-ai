package com.careflowai.config;

import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * CORS is driven by {@code CORS_ALLOWED_ORIGINS} (comma-separated origins or {@code *})
 * and {@code CORS_ALLOW_ALL}. For hackathon / demo deployments where judges may open the
 * app from many URLs, set {@code CORS_ALLOW_ALL=true} or {@code CORS_ALLOWED_ORIGINS=*}.
 * When both are unset/empty, no cross-origin access is allowed (local dev uses the Vite proxy).
 * Exposed as a CorsConfigurationSource bean so Spring Security applies it to preflight too.
 */
@Configuration
public class CorsConfig {

    private final List<String> allowedOrigins;
    private final boolean allowAll;

    public CorsConfig(
            @Value("${cors.allowed-origins:}") String allowedOrigins,
            @Value("${cors.allow-all:false}") boolean allowAll) {
        String trimmed = allowedOrigins.trim();
        this.allowAll = allowAll || "*".equals(trimmed);
        this.allowedOrigins = Arrays.stream(allowedOrigins.split(","))
            .map(String::trim)
            .filter(origin -> !origin.isEmpty() && !"*".equals(origin))
            .toList();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        if (!allowAll && allowedOrigins.isEmpty()) {
            return source;
        }
        CorsConfiguration configuration = new CorsConfiguration();
        if (allowAll) {
            configuration.setAllowedOriginPatterns(List.of("*"));
        } else {
            configuration.setAllowedOriginPatterns(allowedOrigins);
        }
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setMaxAge(3600L);
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
