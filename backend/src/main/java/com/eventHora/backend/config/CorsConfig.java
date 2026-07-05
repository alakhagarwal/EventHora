package com.eventHora.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.List;

/**
 * CORS configuration — allows the React / Next.js frontend to call the backend.
 *
 * Allowed origins:
 *   http://localhost:5173   → Vite (React dev server)
 *   http://localhost:3000   → Next.js dev server / CRA
 *   http://localhost:4173   → Vite preview build
 */
@Configuration
public class CorsConfig {

    private static final List<String> ALLOWED_ORIGINS = List.of(
            "http://localhost:5173",   // Vite (React)
            "http://localhost:4173",   // Vite preview
            "http://localhost:3000"    // Next.js / CRA
    );

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
    public CorsFilter corsFilter() {
        CorsConfiguration config = new CorsConfiguration();

        config.setAllowedOrigins(ALLOWED_ORIGINS);
        config.setAllowedMethods(ALLOWED_METHODS);
        config.setAllowedHeaders(ALLOWED_HEADERS);

        // Allow Authorization header to be read by the frontend
        config.setExposedHeaders(List.of("Authorization"));

        // Allow cookies / credentials to be sent cross-origin
        config.setAllowCredentials(true);

        // Cache preflight (OPTIONS) response for 1 hour
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);   // applies to all API routes

        return new CorsFilter(source);
    }
}
