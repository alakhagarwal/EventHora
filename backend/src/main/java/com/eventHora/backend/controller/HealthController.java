package com.eventHora.backend.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Public health-check endpoint.
 *
 * GET /api/health → 200 OK  { "status": "UP" }
 *
 * Used by UptimeRobot (or any monitor) to keep the Render free-tier instance
 * warm and to confirm the application is running.
 * No authentication required.
 */
@RestController
@RequestMapping("/api/health")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
