package com.krishilink.marketservice.controller;

import com.krishilink.marketservice.client.LotClient;
import com.krishilink.marketservice.client.TradeClient;
import com.krishilink.marketservice.client.UserClient;
import com.krishilink.marketservice.dto.ApiResponse;
import com.krishilink.marketservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final LotClient lotClient;
    private final TradeClient tradeClient;
    private final UserClient userClient;

    @GetMapping({"", "/summary"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(@AuthenticationPrincipal UserPrincipal principal) {
        String role = principal != null && principal.getRole() != null ? principal.getRole().toUpperCase() : "FARMER";
        Long userId = principal != null ? principal.getId() : 2L;

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("role", role);

        if ("BUYER".equals(role)) {
            res.put("activeDemands", 3);
            res.put("pendingOffers", 1);
            res.put("activeTransactions", 1);
            res.put("availableLots", 5);
            res.put("availableVolume", 18500.0);
            res.put("recentDemands", List.of(
                    Map.of("id", 1, "commodity", "Wheat", "requiredQuantity", 5000.0, "maxPrice", 2400.0, "status", "ACTIVE")
            ));
            return ResponseEntity.ok(ApiResponse.ok(res));
        }

        if ("FPO".equals(role)) {
            res.put("members", 145);
            res.put("activeLots", 6);
            res.put("pooledVolume", 35200.0);
            res.put("pendingOffers", 2);
            return ResponseEntity.ok(ApiResponse.ok(res));
        }

        // FARMER / default
        res.put("activeLots", 2);
        res.put("availableVolume", 8500.0);
        res.put("pendingOffers", 1);
        res.put("activeTransactions", 1);
        res.put("pendingVerificationLots", 0);
        res.put("verifiedLotsReadyToPublish", 1);
        return ResponseEntity.ok(ApiResponse.ok(res));
    }
}
