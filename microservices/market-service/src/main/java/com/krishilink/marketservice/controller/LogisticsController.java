package com.krishilink.marketservice.controller;

import com.krishilink.marketservice.dto.ApiResponse;
import com.krishilink.marketservice.entity.LogisticsBooking;
import com.krishilink.marketservice.entity.LogisticsProvider;
import com.krishilink.marketservice.repository.LogisticsBookingRepository;
import com.krishilink.marketservice.repository.LogisticsProviderRepository;
import com.krishilink.marketservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsProviderRepository logisticsProviderRepository;
    private final LogisticsBookingRepository logisticsBookingRepository;

    @GetMapping({"", "/providers"})
    public ResponseEntity<ApiResponse<List<LogisticsProvider>>> getProviders() {
        return ResponseEntity.ok(ApiResponse.ok(logisticsProviderRepository.findAll()));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<LogisticsBooking>>> getBookings(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return ResponseEntity.ok(ApiResponse.ok(List.of()));
        return ResponseEntity.ok(ApiResponse.ok(logisticsBookingRepository.findByUserId(principal.getId())));
    }

    @PostMapping("/book")
    public ResponseEntity<ApiResponse<LogisticsBooking>> bookLogistics(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long providerId = Long.valueOf(body.get("providerId").toString());
        LogisticsProvider lp = logisticsProviderRepository.findById(providerId).orElse(null);

        LogisticsBooking b = LogisticsBooking.builder()
                .providerId(providerId)
                .providerName(lp != null ? lp.getName() : "Logistics Carrier")
                .userId(principal.getId())
                .userName(principal.getUsername())
                .pickupAddress(body.getOrDefault("pickupAddress", "Farm Location").toString())
                .dropoffAddress(body.getOrDefault("dropoffAddress", "Mandi").toString())
                .commodity(body.getOrDefault("commodity", "Produce").toString())
                .quantity(Double.parseDouble(body.getOrDefault("quantity", "1000").toString()))
                .distanceKm(Double.parseDouble(body.getOrDefault("distanceKm", "25").toString()))
                .build();

        b = logisticsBookingRepository.save(b);
        return ResponseEntity.ok(ApiResponse.ok(b, "Transport fleet booked"));
    }
}
