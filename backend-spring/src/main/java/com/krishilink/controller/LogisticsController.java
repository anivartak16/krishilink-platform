package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.LogisticsProvider;
import com.krishilink.repository.LogisticsProviderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsProviderRepository logisticsProviderRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LogisticsProvider>>> getProviders() {
        return ResponseEntity.ok(ApiResponse.ok(logisticsProviderRepository.findAll()));
    }
}
