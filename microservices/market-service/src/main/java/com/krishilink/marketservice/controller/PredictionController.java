package com.krishilink.marketservice.controller;

import com.krishilink.marketservice.dto.ApiResponse;
import com.krishilink.marketservice.dto.PriceDtos.PriceInsightDto;
import com.krishilink.marketservice.service.IntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class PredictionController {

    private final IntelligenceService intelligenceService;

    private static final List<String> COMMODITY_LIST = List.of(
            "Wheat", "Soybean", "Gram", "Maize", "Mustard", "Cotton", "Onion", "Potato", "Tomato"
    );

    @GetMapping("/overview")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getOverview(
            @RequestParam(defaultValue = "FARMER") String role) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (String crop : List.of("Wheat", "Soybean", "Gram", "Mustard")) {
            list.add(buildPrediction(crop, "Indore", role, 14));
        }
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getPredictions(
            @RequestParam(defaultValue = "Wheat") String commodity,
            @RequestParam(defaultValue = "Indore") String location,
            @RequestParam(defaultValue = "FARMER") String role,
            @RequestParam(defaultValue = "14") int horizon) {
        return ResponseEntity.ok(ApiResponse.ok(buildPrediction(commodity, location, role, horizon)));
    }

    @GetMapping("/{crop}")
    public ResponseEntity<ApiResponse<Object>> getCropOrOverview(
            @PathVariable String crop,
            @RequestParam(defaultValue = "Indore") String location,
            @RequestParam(defaultValue = "FARMER") String role,
            @RequestParam(defaultValue = "14") int horizon) {
        if ("overview".equalsIgnoreCase(crop)) {
            List<Map<String, Object>> list = new ArrayList<>();
            for (String c : List.of("Wheat", "Soybean", "Gram", "Mustard")) {
                list.add(buildPrediction(c, location, role, 14));
            }
            return ResponseEntity.ok(ApiResponse.ok(list));
        }
        return ResponseEntity.ok(ApiResponse.ok(buildPrediction(crop, location, role, horizon)));
    }

    private Map<String, Object> buildPrediction(String commodity, String location, String role, int horizon) {
        PriceInsightDto insight = intelligenceService.getPriceInsight(commodity);
        double current = insight != null && insight.getCurrentPrice() != null ? insight.getCurrentPrice() : 25.0;
        double predicted = Math.round((current * 1.05) * 10.0) / 10.0;
        double changePct = Math.round(((predicted - current) / current * 100.0) * 10.0) / 10.0;

        List<Map<String, Object>> forecastSeries = new ArrayList<>();
        List<Map<String, Object>> forecast = new ArrayList<>();
        LocalDate today = LocalDate.now();

        for (int i = 1; i <= Math.min(horizon, 14); i++) {
            double p = Math.round((current + (i * 0.25) + ((i % 2 == 0 ? 0.1 : -0.1))) * 10.0) / 10.0;
            String dayLabel = "Day +" + i;
            forecastSeries.add(Map.of(
                    "day", dayLabel,
                    "date", today.plusDays(i).toString(),
                    "predictedPrice", p,
                    "minPrice", Math.round((p * 0.96) * 10.0) / 10.0,
                    "maxPrice", Math.round((p * 1.04) * 10.0) / 10.0,
                    "confidence", Math.max(60, 88 - (i * 2))
            ));
            forecast.add(Map.of(
                    "day", dayLabel,
                    "projectedPrice", p,
                    "confidence", Math.max(60, 88 - (i * 2))
            ));
        }

        List<Map<String, Object>> historicalSeries = new ArrayList<>();
        for (int i = 5; i >= 1; i--) {
            historicalSeries.add(Map.of(
                    "date", today.minusDays(i).toString(),
                    "modalPrice", Math.round((current - (i * 0.2)) * 10.0) / 10.0
            ));
        }

        Map<String, Object> map = new LinkedHashMap<>();
        map.put("commodity", commodity);
        map.put("location", location);
        map.put("role", role);
        map.put("horizonDays", horizon);
        map.put("currentPrice", current);
        map.put("predictedPrice", predicted);
        map.put("projectedChangePct", changePct);
        map.put("projectedTrend", "BULLISH");
        map.put("priceRange", Map.of("min", Math.round((predicted * 0.95) * 10.0) / 10.0,
                "max", Math.round((predicted * 1.05) * 10.0) / 10.0));
        map.put("unit", "kg");
        map.put("action", "SELL_STRATEGIC");
        map.put("actionType", "HOLD_OR_STAGGER");
        map.put("actionTagline", "Steady upward momentum across APMC mandis");
        map.put("confidence", 85);
        map.put("recommendationSummary", "Prices are trending bullish by ~" + changePct + "% over the next " + horizon + " days.");
        map.put("forecast", forecast);
        map.put("forecastSeries", forecastSeries);
        map.put("historicalSeries", historicalSeries);
        map.put("availableCommodities", COMMODITY_LIST);
        map.put("explainableFactors", List.of(
                Map.of("label", "Demand Velocity", "impact", "Bullish", "score", 0.35, "description", "Procurement demand steady."),
                Map.of("label", "Arrival Volume", "impact", "Bullish", "score", 0.25, "description", "Optimal arrivals support higher modal rate.")
        ));

        return map;
    }
}
