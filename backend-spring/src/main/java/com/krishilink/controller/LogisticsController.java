package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.LogisticsProvider;
import com.krishilink.repository.LogisticsProviderRepository;
import com.krishilink.entity.LogisticsBooking;
import com.krishilink.repository.LogisticsBookingRepository;
import com.krishilink.exception.BadRequestException;
import com.krishilink.exception.ResourceNotFoundException;
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
    private final LogisticsBookingRepository bookingRepository;

    @GetMapping({"", "/providers"})
    public ResponseEntity<ApiResponse<List<LogisticsProvider>>> getProviders() {
        return ResponseEntity.ok(ApiResponse.ok(logisticsProviderRepository.findByAvailabilityTrue()));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<LogisticsBooking>>> getBookings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                bookingRepository.findByUserOrderByCreatedAtDesc(principal.getUser())));
    }

    @PostMapping("/book")
    @Transactional
    public ResponseEntity<ApiResponse<LogisticsBooking>> book(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long providerId = longValue(body.get("providerId"), "providerId");
        double quantity = doubleValue(body.get("quantity"), "quantity");
        double distance = doubleValue(body.get("distanceKm"), "distanceKm");
        String pickup = text(body.get("pickupLocation"), "pickupLocation");
        String delivery = text(body.get("deliveryLocation"), "deliveryLocation");
        LogisticsProvider provider = logisticsProviderRepository.findById(providerId)
                .orElseThrow(() -> new ResourceNotFoundException("Logistics provider not found"));
        if (!Boolean.TRUE.equals(provider.getAvailability()) || provider.getCapacity() == null
                || provider.getCapacity() < quantity) {
            throw new BadRequestException("Provider is unavailable for this quantity");
        }
        double rate = provider.getPricePerKm() == null ? 0 : provider.getPricePerKm();
        LogisticsBooking booking = LogisticsBooking.builder().user(principal.getUser()).provider(provider)
                .pickupLocation(pickup).deliveryLocation(delivery).quantity(quantity)
                .distanceKm(distance).estimatedCost(rate * distance).build();
        return ResponseEntity.ok(ApiResponse.ok(bookingRepository.save(booking), "Logistics booking created"));
    }

    private static Long longValue(Object value, String field) {
        if (value == null) throw new BadRequestException(field + " is required");
        try { return Long.valueOf(value.toString()); }
        catch (NumberFormatException ex) { throw new BadRequestException(field + " must be numeric"); }
    }
    private static double doubleValue(Object value, String field) {
        if (value == null) throw new BadRequestException(field + " is required");
        try { return Double.parseDouble(value.toString()); }
        catch (NumberFormatException ex) { throw new BadRequestException(field + " must be numeric"); }
    }
    private static String text(Object value, String field) {
        if (value == null || value.toString().isBlank()) throw new BadRequestException(field + " is required");
        return value.toString().trim();
    }
}
