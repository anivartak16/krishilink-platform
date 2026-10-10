package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.dto.PriceDtos.*;
import com.krishilink.entity.MarketPrice;
import com.krishilink.repository.MarketPriceRepository;
import com.krishilink.service.IntelligenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class PriceController {

    private final IntelligenceService intelligenceService;
    private final MarketPriceRepository marketPriceRepository;

    @GetMapping("/api/prices")
    public ResponseEntity<ApiResponse<PriceInsightDto>> getPriceInsight(
            @RequestParam(defaultValue = "Wheat") String commodity) {
        PriceInsightDto insight = intelligenceService.getPriceInsight(commodity);
        return ResponseEntity.ok(ApiResponse.ok(insight));
    }

    @GetMapping("/api/prices/trends")
    public ResponseEntity<ApiResponse<PriceInsightDto>> getPriceTrends(
            @RequestParam(defaultValue = "Wheat") String commodity) {
        PriceInsightDto insight = intelligenceService.getPriceInsight(commodity);
        return ResponseEntity.ok(ApiResponse.ok(insight));
    }

    @GetMapping("/api/prices/opportunity")
    public ResponseEntity<ApiResponse<PriceOpportunityAlertDto>> getOpportunity(
            @RequestParam(defaultValue = "Wheat") String commodity) {
        PriceOpportunityAlertDto alert = intelligenceService.getPriceOpportunityAlert(commodity);
        return ResponseEntity.ok(ApiResponse.ok(alert));
    }

    @GetMapping("/api/prices/advice")
    public ResponseEntity<ApiResponse<SellAdviceDto>> getAdvice(
            @RequestParam(defaultValue = "Wheat") String commodity) {
        SellAdviceDto advice = intelligenceService.getSellAdvice(commodity);
        return ResponseEntity.ok(ApiResponse.ok(advice));
    }

    @GetMapping("/api/marketPrice")
    public ResponseEntity<ApiResponse<List<MarketPrice>>> getMarketPrices(
            @RequestParam(required = false) String commodity) {
        List<MarketPrice> list;
        if (commodity != null && !commodity.isBlank()) {
            list = marketPriceRepository.findByCommodityIgnoreCaseOrderByPriceDateDesc(commodity);
        } else {
            list = marketPriceRepository.findAll();
        }
        return ResponseEntity.ok(ApiResponse.ok(list));
    }
}
