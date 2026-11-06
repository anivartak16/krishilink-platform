package com.krishilink.lotservice.controller;

import com.krishilink.lotservice.dto.ApiResponse;
import com.krishilink.lotservice.dto.LotDto;
import com.krishilink.lotservice.entity.Lot;
import com.krishilink.lotservice.security.UserPrincipal;
import com.krishilink.lotservice.service.LotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/lots")
@RequiredArgsConstructor
public class LotController {

    private final LotService lotService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Lot>>> getLots(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String commodity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String farmingType,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Long owner,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Boolean mine) {
        if (Boolean.TRUE.equals(mine) && principal != null) {
            owner = principal.getId();
        }
        List<Lot> lots = lotService.getLots(commodity, status, farmingType, minPrice, maxPrice, owner, search);
        return ResponseEntity.ok(ApiResponse.ok(lots));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Lot>> getLotById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(lotService.getLotById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Lot>> createLot(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody LotDto dto) {
        Lot lot = lotService.createLot(principal, dto);
        return ResponseEntity.ok(ApiResponse.ok(lot, "Lot registered successfully"));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<Lot>> publishLot(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Lot lot = lotService.publishLot(id, principal);
        return ResponseEntity.ok(ApiResponse.ok(lot, "Lot published to active marketplace"));
    }

    @PostMapping("/{id}/verify-request")
    public ResponseEntity<ApiResponse<Lot>> resubmitForVerification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Lot lot = lotService.resubmitLot(id, principal);
        return ResponseEntity.ok(ApiResponse.ok(lot, "Harvest sample resubmitted for testing"));
    }
}
