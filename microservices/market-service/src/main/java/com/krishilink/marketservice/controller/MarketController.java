package com.krishilink.marketservice.controller;

import com.krishilink.marketservice.dto.ApiResponse;
import com.krishilink.marketservice.entity.Market;
import com.krishilink.marketservice.repository.MarketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/markets")
@RequiredArgsConstructor
public class MarketController {

    private final MarketRepository marketRepository;

    @GetMapping({"", "/nearby"})
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getNearby(
            @RequestParam(defaultValue = "Wheat") String commodity) {
        List<Market> markets = marketRepository.findAll();
        List<Map<String, Object>> list = new ArrayList<>();
        double base = "Soybean".equalsIgnoreCase(commodity) ? 4850.0 : 2450.0;

        for (int i = 0; i < markets.size(); i++) {
            Market m = markets.get(i);
            double price = base + (i * 15.0) - 10.0;
            list.add(Map.of(
                    "id", m.getId(),
                    "_id", m.getId().toString(),
                    "name", m.getName(),
                    "district", m.getDistrict() != null ? m.getDistrict() : "Indore",
                    "state", m.getState() != null ? m.getState() : "Madhya Pradesh",
                    "commodity", commodity,
                    "price", price,
                    "distanceKm", 12.0 + (i * 8.0),
                    "netRealisation", price - (12.0 + i * 8.0) * 0.45,
                    "trend", i % 2 == 0 ? "UP" : "STABLE"
            ));
        }
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Market>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(marketRepository.findById(id).orElse(null)));
    }
}
