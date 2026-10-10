package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.dto.PriceDtos.PriceInsightDto;
import com.krishilink.service.IntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final IntelligenceService intelligenceService;

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPredictions(
            @RequestParam(defaultValue = "Wheat") String commodity) {
        PriceInsightDto insight = intelligenceService.getPriceInsight(commodity);
        double base = insight != null ? insight.getCurrentPrice() : 24.5;

        List<Map<String, Object>> forecast = new ArrayList<>();
        for (int i = 1; i <= 7; i++) {
            double projected = Math.round((base + (i * 0.35) + ((i % 2 == 0 ? 1 : -1) * 0.15)) * 100.0) / 100.0;
            forecast.add(Map.of(
                    "day", "Day +" + i,
                    "projectedPrice", projected,
                    "confidence", 85 - (i * 3)
            ));
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "commodity", commodity,
                "currentPrice", base,
                "projectedTrend", "BULLISH",
                "forecast", forecast
        )));
    }

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOverview(
            @RequestParam(defaultValue = "FARMER") String role) {
        List<Map<String, Object>> result = List.of("Wheat", "Soybean", "Onion", "Mustard").stream()
                .map(crop -> prediction(crop, "Indore", role, 14)).toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/{commodity}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCommodityPrediction(
            @PathVariable String commodity,
            @RequestParam(defaultValue = "Indore") String location,
            @RequestParam(defaultValue = "FARMER") String role,
            @RequestParam(defaultValue = "14") int horizon) {
        if (horizon < 1 || horizon > 365) {
            throw new com.krishilink.exception.BadRequestException("horizon must be between 1 and 365 days");
        }
        return ResponseEntity.ok(ApiResponse.ok(prediction(commodity, location, role, horizon)));
    }

    private Map<String, Object> prediction(String commodity, String location, String role, int horizon) {
        PriceInsightDto insight = intelligenceService.getPriceInsight(commodity);
        double current = insight != null && insight.getCurrentPrice() != null ? insight.getCurrentPrice() : 24.5;
        double change = insight != null && insight.getChangePercentage() != null ? insight.getChangePercentage() : 2.5;
        double predicted = Math.round(current * (1 + (change / 100.0) * Math.min(horizon, 30) / 30.0) * 100.0) / 100.0;
        List<Map<String, Object>> forecast = new ArrayList<>();
        for (int i = 0; i <= horizon; i += Math.max(1, horizon / 7)) {
            double value = Math.round((current + (predicted - current) * i / horizon) * 100.0) / 100.0;
            forecast.add(Map.of("day", i == 0 ? "Today" : "+" + i + "d", "projectedPrice", value,
                    "upperBound", value * 1.03, "lowerBound", value * 0.97));
        }
        String action = change > 3 ? "SELL NOW" : change < -3 ? "HOLD" : "HOLD";
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("commodity", commodity);
        result.put("location", location);
        result.put("role", role.toUpperCase());
        result.put("horizonDays", horizon);
        result.put("currentPrice", current);
        result.put("predictedPrice", predicted);
        result.put("projectedChangePct", Math.round(((predicted - current) / current) * 1000.0) / 10.0);
        result.put("priceRange", Map.of("min", predicted * 0.97, "max", predicted * 1.03));
        result.put("unit", "kg");
        result.put("action", action);
        result.put("actionType", change > 3 ? "urgent" : "neutral");
        result.put("confidence", insight == null ? 60 : 82);
        result.put("forecastSeries", forecast);
        result.put("historicalSeries", insight == null ? List.of() : insight.getHistory());
        result.put("timestamp", java.time.Instant.now().toString());
        return result;
    }
}
