package com.krishilink.marketservice.controller;

import com.krishilink.marketservice.dto.ApiResponse;
import com.krishilink.marketservice.dto.PriceDtos.MarketRecommendationDto;
import com.krishilink.marketservice.entity.Market;
import com.krishilink.marketservice.repository.MarketRepository;
import com.krishilink.marketservice.service.IntelligenceService;
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

    @GetMapping({"", "/nearby"})
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
        Market market = marketRepository.findById(id).orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(market));
    }
}
