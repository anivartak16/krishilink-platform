package com.krishilink.marketservice.controller;

import com.krishilink.marketservice.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/predictions")
public class PredictionController {

    @GetMapping({"", "/overview", "/{crop}"})
    public ResponseEntity<ApiResponse<Object>> getPrediction(
            @PathVariable(required = false) String crop,
            @RequestParam(defaultValue = "Wheat") String commodity) {
        String target = crop != null && !"overview".equalsIgnoreCase(crop) ? crop : commodity;
        double base = "Soybean".equalsIgnoreCase(target) ? 4850.0 : 2450.0;

        List<Map<String, Object>> forecast = new ArrayList<>();
        for (int i = 1; i <= 14; i++) {
            forecast.add(Map.of(
                    "date", LocalDate.now().plusDays(i).toString(),
                    "predictedPrice", base + (i * 12.0)
            ));
        }

        Map<String, Object> data = Map.of(
                "crop", target,
                "currentPrice", base,
                "forecast", forecast,
                "advice", "SELL_WINDOW_OPEN",
                "confidence", 85
        );
        return ResponseEntity.ok(ApiResponse.ok(data));
    }
}
