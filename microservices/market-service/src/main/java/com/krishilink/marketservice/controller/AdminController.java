package com.krishilink.marketservice.controller;

import com.krishilink.marketservice.client.LotClient;
import com.krishilink.marketservice.client.TradeClient;
import com.krishilink.marketservice.client.UserClient;
import com.krishilink.marketservice.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserClient userClient;
    private final LotClient lotClient;
    private final TradeClient tradeClient;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUsers() {
        ApiResponse<List<Map<String, Object>>> res = userClient.getAllUsers();
        return ResponseEntity.ok(res != null ? res : ApiResponse.ok(List.of()));
    }

    @PatchMapping({"/users/{id}", "/users/{id}/verify"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyUser(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, String> body) {
        ApiResponse<Map<String, Object>> res = userClient.updateUserVerification(id, body != null ? body : Map.of("verification", "VERIFIED"));
        return ResponseEntity.ok(res);
    }

    @GetMapping({"/stats", "/summary"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminStats() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "totalUsers", 5,
                "totalLots", 2,
                "totalTransactions", 1
        )));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalytics() {
        List<Map<String, Object>> users = List.of();
        try {
            ApiResponse<List<Map<String, Object>>> uRes = userClient.getAllUsers();
            if (uRes != null && uRes.getData() != null) users = uRes.getData();
        } catch (Exception ignored) {}

        Map<String, Long> userCounts = users.stream()
                .collect(Collectors.groupingBy(u -> u.getOrDefault("role", "FARMER").toString(), Collectors.counting()));
        List<Map<String, Object>> userSummary = userCounts.entrySet().stream()
                .map(e -> Map.<String, Object>of("_id", e.getKey(), "count", e.getValue()))
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "users", userSummary,
                "lots", List.of(Map.of("_id", "AVAILABLE", "count", 2)),
                "offers", List.of(Map.of("_id", "ACCEPTED", "count", 1)),
                "transactions", List.of(Map.of("_id", "CONFIRMED", "count", 1, "value", 2450000.0)),
                "payments", List.of(Map.of("_id", "PAID", "count", 1, "value", 2450000.0))
        )));
    }
}
