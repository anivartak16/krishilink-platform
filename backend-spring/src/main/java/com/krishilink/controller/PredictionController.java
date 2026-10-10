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
}
