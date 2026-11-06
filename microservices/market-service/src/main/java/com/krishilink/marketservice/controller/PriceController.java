package com.krishilink.marketservice.controller;

import com.krishilink.marketservice.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prices")
public class PriceController {

    @GetMapping("/trends")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTrends(
            @RequestParam(defaultValue = "Wheat") String commodity) {
        List<Map<String, Object>> history = new ArrayList<>();
        double base = "Soybean".equalsIgnoreCase(commodity) ? 4850.0 : 2420.0;
        for (int i = 14; i >= 0; i--) {
            history.add(Map.of(
                    "date", LocalDate.now().minusDays(i).toString(),
                    "price", base + (Math.sin(i) * 30.0)
            ));
        }
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "commodity", commodity,
                "currentPrice", base,
                "history", history,
                "advice", Map.of(
                        "action", "HOLD",
                        "confidence", 82,
                        "forecastPeakPrice", base + 110.0,
                        "expectedDays", 8
                )
        )));
    }
}
