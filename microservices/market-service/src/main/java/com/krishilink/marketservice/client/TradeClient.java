package com.krishilink.marketservice.client;

import com.krishilink.marketservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "trade-service")
public interface TradeClient {
    @GetMapping("/api/trade-internal/offers")
    ApiResponse<List<Map<String, Object>>> getAllOffers();

    @GetMapping("/api/trade-internal/transactions")
    ApiResponse<List<Map<String, Object>>> getAllTransactions();

    @GetMapping("/api/trade-internal/payments")
    ApiResponse<List<Map<String, Object>>> getAllPayments();
}
