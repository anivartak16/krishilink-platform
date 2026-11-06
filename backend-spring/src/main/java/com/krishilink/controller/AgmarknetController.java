package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.service.AgmarknetService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/marketPrice")
@RequiredArgsConstructor
public class AgmarknetController {

    private final AgmarknetService agmarknetService;

    @GetMapping("/live")
    public ResponseEntity<ApiResponse<AgmarknetService.FetchResult>> fetchLivePrices(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String commodity,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String market,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate arrivalDate,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(ApiResponse.ok(
                agmarknetService.fetchPrices(state, commodity, district, market, arrivalDate, limit),
                "Live AGMARKNET prices fetched successfully"));
    }
}
