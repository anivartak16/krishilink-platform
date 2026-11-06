package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.StorageBooking;
import com.krishilink.entity.Warehouse;
import com.krishilink.exception.BadRequestException;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.repository.StorageBookingRepository;
import com.krishilink.repository.WarehouseRepository;
import com.krishilink.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/storage")
@RequiredArgsConstructor
public class StorageController {

    private final WarehouseRepository warehouseRepository;
    private final StorageBookingRepository storageBookingRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Warehouse>>> getWarehouses(
            @RequestParam(required = false) String location) {
        List<Warehouse> list = location != null ?
                warehouseRepository.findByLocationContainingIgnoreCase(location) :
                warehouseRepository.findAll();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PostMapping("/book")
    @Transactional
    public ResponseEntity<ApiResponse<StorageBooking>> bookStorage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long warehouseId = Long.valueOf(body.get("warehouseId").toString());
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found"));

        Double qty = Double.valueOf(body.get("quantity").toString());
        int days = body.get("days") != null ? Integer.parseInt(body.get("days").toString()) : 1;

        if (warehouse.getAvailableCapacity() != null && qty > warehouse.getAvailableCapacity()) {
            throw new BadRequestException("Requested quantity exceeds available capacity");
        }

        double cost = qty * days * (warehouse.getPricePerUnitPerDay() != null ? warehouse.getPricePerUnitPerDay() : 2.5);

        StorageBooking booking = StorageBooking.builder()
                .warehouse(warehouse)
                .user(principal.getUser())
                .quantity(qty)
                .days(days)
                .estimatedCost(cost)
                .status("CONFIRMED")
                .build();
        booking = storageBookingRepository.save(booking);

        if (warehouse.getAvailableCapacity() != null) {
            warehouse.setAvailableCapacity(Math.max(0, warehouse.getAvailableCapacity() - qty));
            warehouseRepository.save(warehouse);
        }

        return ResponseEntity.ok(ApiResponse.ok(booking, "Storage booked successfully"));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<StorageBooking>>> getBookings(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<StorageBooking> bookings = storageBookingRepository.findByUserOrderByCreatedAtDesc(principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok(bookings));
    }
}
