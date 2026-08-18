package com.eventHora.backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

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

        List<String> allowedOrigins = Arrays.asList(allowedOriginsRaw.split(","));
        config.setAllowedOrigins(allowedOrigins);
        config.setAllowedMethods(ALLOWED_METHODS);
        config.setAllowedHeaders(ALLOWED_HEADERS);

        config.setExposedHeaders(List.of("Authorization"));

        config.setAllowCredentials(true);

        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);

        return source;
    }
}
