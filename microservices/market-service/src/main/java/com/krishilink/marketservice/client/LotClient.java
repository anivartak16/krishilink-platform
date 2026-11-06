package com.krishilink.marketservice.client;

import com.krishilink.marketservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;

@FeignClient(name = "lot-service")
public interface LotClient {
    @GetMapping("/api/lots")
    ApiResponse<List<Map<String, Object>>> getAllLots();
}
