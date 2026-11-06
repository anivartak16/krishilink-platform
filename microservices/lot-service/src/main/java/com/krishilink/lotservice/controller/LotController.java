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
import java.util.Map;

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

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Lot>> updateLot(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody LotDto dto) {
        Lot lot = lotService.updateLot(id, principal, dto);
        return ResponseEntity.ok(ApiResponse.ok(lot, "Lot updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteLot(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        lotService.deleteLot(id, principal);
        return ResponseEntity.ok(ApiResponse.ok("Lot cancelled successfully"));
    }

    @PostMapping("/{id}/publish")
    public ResponseEntity<ApiResponse<Lot>> publishLot(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Lot lot = lotService.publishLot(id, principal);
        return ResponseEntity.ok(ApiResponse.ok(lot, "Lot published to active marketplace"));
    }

    @PostMapping("/{id}/verify-request")
    public ResponseEntity<ApiResponse<Lot>> sendForVerification(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) Map<String, String> body) {
        String notes = body != null ? body.get("notes") : null;
        Lot lot = lotService.sendForVerification(id, principal, notes);
        return ResponseEntity.ok(ApiResponse.ok(lot, "Lot submitted for quality verification"));
    }

    @PostMapping("/by-owners")
    public ResponseEntity<ApiResponse<List<Lot>>> getLotsByOwners(@RequestBody List<Long> ownerIds) {
        return ResponseEntity.ok(ApiResponse.ok(lotService.getLotsByOwnerIds(ownerIds)));
    }

    @PostMapping("/aggregate")
    public ResponseEntity<ApiResponse<Lot>> aggregateLots(@RequestBody Map<String, Object> body) {
        Long fpoId = Long.valueOf(body.get("fpoId").toString());
        String fpoName = (String) body.get("fpoName");
        String location = (String) body.get("location");
        Double lat = body.get("latitude") != null ? Double.valueOf(body.get("latitude").toString()) : null;
        Double lon = body.get("longitude") != null ? Double.valueOf(body.get("longitude").toString()) : null;
        List<?> rawIds = (List<?>) body.get("lotIds");
        List<Long> lotIds = rawIds.stream().map(o -> Long.valueOf(o.toString())).toList();
        Double expectedPrice = body.get("expectedPrice") != null ? Double.valueOf(body.get("expectedPrice").toString()) : null;

        Lot lot = lotService.aggregateLots(fpoId, fpoName, location, lat, lon, lotIds, expectedPrice);
        return ResponseEntity.ok(ApiResponse.ok(lot, "FPO aggregated lot created successfully"));
    }
}
