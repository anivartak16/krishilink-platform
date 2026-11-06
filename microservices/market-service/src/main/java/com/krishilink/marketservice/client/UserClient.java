package com.krishilink.marketservice.client;

import com.krishilink.marketservice.dto.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

@FeignClient(name = "user-service")
public interface UserClient {
    @GetMapping("/api/users")
    ApiResponse<List<Map<String, Object>>> getAllUsers();

    @PatchMapping("/api/users/{id}/verification")
    ApiResponse<Map<String, Object>> updateUserVerification(@PathVariable("id") Long id, @RequestBody Map<String, String> body);
}
