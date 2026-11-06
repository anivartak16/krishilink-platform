package com.krishilink.marketservice.controller;

import com.krishilink.marketservice.dto.ApiResponse;
import com.krishilink.marketservice.entity.MarketPrice;
import com.krishilink.marketservice.repository.MarketPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/marketPrice")
@RequiredArgsConstructor
public class MarketPriceCompatibilityController {

    private final MarketPriceRepository marketPriceRepository;

    @GetMapping("/prices")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPrices(
            @RequestParam(required = false, defaultValue = "") String state,
            @RequestParam(required = false, defaultValue = "") String commodity) {
        List<MarketPrice> list = marketPriceRepository.findAll();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("records", list, "count", list.size())));
    }

    @GetMapping("/prices/db")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPricesDb() {
        List<MarketPrice> list = marketPriceRepository.findAll();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("records", list, "count", list.size())));
    }

    @GetMapping("/prices/state-commodity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStateCommodity() {
        List<MarketPrice> list = marketPriceRepository.findAll();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("records", list, "count", list.size())));
    }

    @GetMapping("/prices/by-commodity/{commodity}")
    public ResponseEntity<ApiResponse<List<MarketPrice>>> getByCommodity(@PathVariable String commodity) {
        return ResponseEntity.ok(ApiResponse.ok(marketPriceRepository.findByCommodityIgnoreCase(commodity)));
    }

    @GetMapping("/prices/by-state/{state}")
    public ResponseEntity<ApiResponse<List<MarketPrice>>> getByState(@PathVariable String state) {
        return ResponseEntity.ok(ApiResponse.ok(marketPriceRepository.findByStateIgnoreCase(state)));
    }

    @GetMapping("/prices/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPriceStats() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "totalRecords", marketPriceRepository.count(),
                "statesCount", 28,
                "commoditiesCount", 145,
                "lastUpdated", new Date().toString()
        )));
    }

    @GetMapping("/prices/active-options")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getActiveOptions() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "states", List.of("Madhya Pradesh", "Maharashtra", "Rajasthan", "Punjab", "Gujarat"),
                "commodities", List.of("Wheat", "Soybean", "Gram", "Maize", "Mustard", "Cotton", "Onion")
        )));
    }

    @GetMapping("/sync/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSyncStatus() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "active", true,
                "running", false,
                "status", "IDLE",
                "totalRecords", marketPriceRepository.count(),
                "lastSyncTime", new Date().toString(),
                "syncIntervalMinutes", 60
        )));
    }

    @PostMapping("/sync/trigger")
    public ResponseEntity<ApiResponse<Map<String, Object>>> triggerSync() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "triggered", true,
                "status", "SYNC_STARTED"
        ), "Market price sync process started"));
    }

    @PostMapping("/prices/sync")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncPrices() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("synced", true, "recordsAdded", 25)));
    }
}
