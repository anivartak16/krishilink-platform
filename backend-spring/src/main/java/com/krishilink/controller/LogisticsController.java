package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.LogisticsBooking;
import com.krishilink.entity.LogisticsProvider;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.repository.LogisticsBookingRepository;
import com.krishilink.repository.LogisticsProviderRepository;
import com.krishilink.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/logistics")
@RequiredArgsConstructor
public class LogisticsController {

    private final LogisticsProviderRepository logisticsProviderRepository;
    private final LogisticsBookingRepository logisticsBookingRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<LogisticsProvider>>> getProviders() {
        return ResponseEntity.ok(ApiResponse.ok(logisticsProviderRepository.findAll()));
    }

    @PostMapping("/book")
    @Transactional
    public ResponseEntity<ApiResponse<LogisticsBooking>> bookLogistics(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long providerId = Long.valueOf(body.get("providerId").toString());
        LogisticsProvider provider = logisticsProviderRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Logistics provider not found"));

        Double qty = body.get("quantity") != null ? Double.valueOf(body.get("quantity").toString()) : 100.0;
        Double dist = body.get("distanceKm") != null ? Double.valueOf(body.get("distanceKm").toString()) : 50.0;
        String pickup = body.get("pickupLocation") != null ? body.get("pickupLocation").toString() : principal.getUser().getLocation();
        String delivery = body.get("deliveryLocation") != null ? body.get("deliveryLocation").toString() : "Regional Mandi";
        Double cost = body.get("estimatedCost") != null ?
                Double.valueOf(body.get("estimatedCost").toString()) :
                (provider.getPricePerKm() != null ? provider.getPricePerKm() * dist : 15.0 * dist);

        LogisticsBooking booking = LogisticsBooking.builder()
                .provider(provider)
                .user(principal.getUser())
                .pickupLocation(pickup)
                .deliveryLocation(delivery)
                .quantity(qty)
                .distanceKm(dist)
                .estimatedCost(cost)
                .status("BOOKED")
                .build();
        booking = logisticsBookingRepository.save(booking);

        return ResponseEntity.ok(ApiResponse.ok(booking, "Logistics booked successfully"));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<LogisticsBooking>>> getBookings(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<LogisticsBooking> bookings = logisticsBookingRepository.findByUserOrderByCreatedAtDesc(principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok(bookings));
    }
}
