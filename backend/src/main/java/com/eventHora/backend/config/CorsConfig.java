package com.eventHora.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration — allows the React / Next.js frontend to call the backend.
 *
 * Exposes a CorsConfigurationSource bean so Spring Security's filter chain
 * can apply it via .cors(Customizer.withDefaults()) in SecurityConfig.
 * This ensures CORS headers are set before Spring Security processes the
 * request, meaning OPTIONS preflight requests are handled correctly.
 *
 * Allowed origins are driven by the cors.allowed-origins property, which is
 * set via the CORS_ALLOWED_ORIGINS environment variable on production (Render).
 * Multiple origins are comma-separated, e.g.:
 *   CORS_ALLOWED_ORIGINS=https://eventhora.vercel.app,https://custom-domain.com
 *
 * Defaults (local dev):
 *   http://localhost:3000  → Next.js
 *   http://localhost:5173  → Vite (React)
 *   http://localhost:4173  → Vite preview build
 */
@Configuration
public class CorsConfig {

    @Value("${cors.allowed-origins:http://localhost:3000,http://localhost:5173,http://localhost:4173}")
    private String allowedOriginsRaw;

    private static final List<String> ALLOWED_METHODS = List.of(
            "GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"
    );

    private static final List<String> ALLOWED_HEADERS = List.of(
            "Authorization",
            "Content-Type",
            "Accept",
            "Origin",
            "X-Requested-With"
    );

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        // Split comma-separated origins from the env var / property
        List<String> allowedOrigins = Arrays.asList(allowedOriginsRaw.split(","));
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(ALLOWED_METHODS);
        config.setAllowedHeaders(ALLOWED_HEADERS);

        // Allow the frontend to read the Authorization header from responses
        config.setExposedHeaders(List.of("Authorization"));

        // Allow cookies / credentials to be sent cross-origin
        config.setAllowCredentials(true);

        // Cache preflight (OPTIONS) response for 1 hour — avoids a preflight on every request
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return source;
    }
}
