package com.krishilink.userservice.client;

import com.krishilink.userservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "lot-service")
public interface LotClient {

    @PostMapping("/api/lots/by-owners")
    ApiResponse<List<Map<String, Object>>> getLotsByOwners(@RequestBody List<Long> ownerIds);

    @PostMapping("/api/lots/aggregate")
    ApiResponse<Map<String, Object>> aggregateLots(@RequestBody Map<String, Object> body);
}
