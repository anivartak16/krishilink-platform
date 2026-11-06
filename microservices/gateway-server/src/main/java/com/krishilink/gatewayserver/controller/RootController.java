package com.krishilink.gatewayserver.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class RootController {

    @GetMapping("/")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(Map.of(
                "service", "KrishiLink Cloud Gateway",
                "status", "UP",
                "architecture", "Spring Cloud Microservices (EazyBytes pattern)",
                "port", 5000
        ));
    }

    @GetMapping("/api/health")
    public ResponseEntity<Map<String, Object>> health() {
        return ResponseEntity.ok(Map.of(
                "status", "UP",
                "gateway", "HEALTHY",
                "timestamp", System.currentTimeMillis()
        ));
    }
}
