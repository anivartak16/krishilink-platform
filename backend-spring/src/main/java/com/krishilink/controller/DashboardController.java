package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.security.UserPrincipal;
import com.krishilink.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping({"", "/summary"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getSummary(
            @AuthenticationPrincipal UserPrincipal principal) {
        Map<String, Object> summary = dashboardService.getSummary(principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok(summary));
    }
}
