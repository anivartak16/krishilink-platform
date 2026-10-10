package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.dto.PriceDtos.MarketRecommendationDto;
import com.krishilink.entity.Market;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.repository.MarketRepository;
import com.krishilink.service.IntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/markets")
@RequiredArgsConstructor
public class MarketController {

    private final IntelligenceService intelligenceService;
    private final MarketRepository marketRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MarketRecommendationDto>>> getMarkets(
            @RequestParam(defaultValue = "Wheat") String commodity,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = "FARMER") String role) {
        List<MarketRecommendationDto> list = intelligenceService.getMarketsFor(commodity, district, state, location, role);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Market>> getMarketById(@PathVariable Long id) {
        Market market = marketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Market not found"));
        return ResponseEntity.ok(ApiResponse.ok(market));
    }
}
