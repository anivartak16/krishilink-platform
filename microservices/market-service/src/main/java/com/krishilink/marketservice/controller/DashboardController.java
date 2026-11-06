package com.krishilink.marketservice.controller;

import com.krishilink.marketservice.dto.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    @GetMapping({"", "/summary"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "activeLots", 12,
                "pendingOffers", 4,
                "verifiedTradeVolume", 52400.0,
                "escrowProtectedAmount", 1284000.0
        )));
    }
}
