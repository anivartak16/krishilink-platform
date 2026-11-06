package com.krishilink.demandservice.client;

import com.krishilink.demandservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Map;

@FeignClient(name = "lot-service")
public interface LotClient {
    @GetMapping("/api/lots")
    ApiResponse<List<Map<String, Object>>> getLots(
            @RequestParam(value = "commodity", required = false) String commodity,
            @RequestParam(value = "status", required = false) String status);
}
