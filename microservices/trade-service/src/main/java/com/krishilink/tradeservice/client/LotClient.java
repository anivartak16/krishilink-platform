package com.krishilink.tradeservice.client;

import com.krishilink.tradeservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.Map;

@FeignClient(name = "lot-service")
public interface LotClient {
    @GetMapping("/api/lots/{id}")
    ApiResponse<Map<String, Object>> getLotById(@PathVariable("id") Long id);
}
