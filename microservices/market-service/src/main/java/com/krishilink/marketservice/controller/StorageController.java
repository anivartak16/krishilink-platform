package com.krishilink.marketservice.controller;

import com.krishilink.marketservice.dto.ApiResponse;
import com.krishilink.marketservice.entity.StorageBooking;
import com.krishilink.marketservice.entity.Warehouse;
import com.krishilink.marketservice.repository.StorageBookingRepository;
import com.krishilink.marketservice.repository.WarehouseRepository;
import com.krishilink.marketservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
    public ResponseEntity<ApiResponse<List<Warehouse>>> getWarehouses() {
        return ResponseEntity.ok(ApiResponse.ok(warehouseRepository.findAll()));
    }

    @GetMapping("/bookings")
    public ResponseEntity<ApiResponse<List<StorageBooking>>> getBookings(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) return ResponseEntity.ok(ApiResponse.ok(List.of()));
        return ResponseEntity.ok(ApiResponse.ok(storageBookingRepository.findByUserId(principal.getId())));
    }

    @PostMapping("/book")
    public ResponseEntity<ApiResponse<StorageBooking>> bookStorage(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long warehouseId = Long.valueOf(body.get("warehouseId").toString());
        Warehouse w = warehouseRepository.findById(warehouseId).orElse(null);

        StorageBooking booking = StorageBooking.builder()
                .warehouseId(warehouseId)
                .warehouseName(w != null ? w.getName() : "Cold Storage")
                .userId(principal.getId())
                .userName(principal.getUsername())
                .commodity(body.getOrDefault("commodity", "Produce").toString())
                .quantity(Double.parseDouble(body.getOrDefault("quantity", "1000").toString()))
                .days(Integer.parseInt(body.getOrDefault("days", "30").toString()))
                .notes(body.getOrDefault("notes", "").toString())
                .build();

        booking = storageBookingRepository.save(booking);
        return ResponseEntity.ok(ApiResponse.ok(booking, "Cold storage space reserved"));
    }
}
