package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.Warehouse;
import com.krishilink.repository.WarehouseRepository;
import com.krishilink.entity.StorageBooking;
import com.krishilink.repository.StorageBookingRepository;
import com.krishilink.exception.BadRequestException;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    private final WarehouseRepository warehouseRepository;
    private final StorageBookingRepository bookingRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Warehouse>>> getWarehouses(
            @RequestParam(required = false) String location) {
        List<Warehouse> list = location != null ?
                warehouseRepository.findByLocationContainingIgnoreCase(location) :
                warehouseRepository.findAll();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<StorageBooking>>> getBookings(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(
                bookingRepository.findByUserOrderByCreatedAtDesc(principal.getUser())));
    }

    @PostMapping("/book")
    @Transactional
    public ResponseEntity<ApiResponse<StorageBooking>> book(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long warehouseId = longValue(body.get("warehouseId"), "warehouseId");
        double quantity = doubleValue(body.get("quantity"), "quantity");
        int days = (int) doubleValue(body.get("days"), "days");
        if (quantity <= 0 || days <= 0) throw new BadRequestException("Quantity and days must be positive");
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));
        if (warehouse.getAvailableCapacity() == null || warehouse.getAvailableCapacity() < quantity) {
            throw new BadRequestException("Requested quantity exceeds available storage capacity");
        }
        double price = warehouse.getPricePerUnitPerDay() == null ? 0 : warehouse.getPricePerUnitPerDay();
        StorageBooking booking = StorageBooking.builder().user(principal.getUser()).warehouse(warehouse)
                .quantity(quantity).days(days).estimatedCost(price * quantity * days).build();
        warehouse.setAvailableCapacity(warehouse.getAvailableCapacity() - quantity);
        warehouseRepository.save(warehouse);
        return ResponseEntity.ok(ApiResponse.ok(bookingRepository.save(booking), "Storage booking created"));
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
}
