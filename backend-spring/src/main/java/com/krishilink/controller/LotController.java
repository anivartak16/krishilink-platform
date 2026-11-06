package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.dto.LotDto;
import com.krishilink.entity.Lot;
import com.krishilink.security.UserPrincipal;
import com.krishilink.service.LotService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/lots")
@RequiredArgsConstructor
public class LotController {

    private final LotService lotService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Lot>>> getLots(
            @RequestParam(required = false) String commodity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String farmingType,
            @RequestParam(required = false) Double minPrice,
            @RequestParam(required = false) Double maxPrice,
            @RequestParam(required = false) Long owner,
            @RequestParam(required = false) String search) {
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
        Lot lot = lotService.createLot(principal.getUser(), dto);
        return ResponseEntity.ok(ApiResponse.ok(lot, "Lot created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Lot>> updateLot(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody LotDto dto) {
        Lot lot = lotService.updateLot(id, principal.getUser(), dto);
        return ResponseEntity.ok(ApiResponse.ok(lot, "Lot updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteLot(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        lotService.deleteLot(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok("Lot cancelled successfully"));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<Lot>> publishLot(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Lot lot = lotService.publishLot(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok(lot, "Lot published successfully"));
    }

    @PostMapping("/{id}/verify-request")
    public ResponseEntity<ApiResponse<Lot>> sendForVerification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        Lot lot = lotService.sendForVerification(id, principal.getUser(), notes);
        return ResponseEntity.ok(ApiResponse.ok(lot, "Lot submitted for quality verification"));
    }
}
