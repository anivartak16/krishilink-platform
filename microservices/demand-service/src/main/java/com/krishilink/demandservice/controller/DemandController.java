package com.krishilink.demandservice.controller;

import com.krishilink.demandservice.dto.ApiResponse;
import com.krishilink.demandservice.dto.DemandDto;
import com.krishilink.demandservice.entity.Demand;
import com.krishilink.demandservice.security.UserPrincipal;
import com.krishilink.demandservice.service.DemandService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/demands")
@RequiredArgsConstructor
public class DemandController {

    private final DemandService demandService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Demand>>> getDemands(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String commodity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long buyer,
            @RequestParam(required = false) Boolean mine) {
        if (Boolean.TRUE.equals(mine) && principal != null) {
            buyer = principal.getId();
        }
        List<Demand> demands = demandService.getDemands(commodity, status, buyer);
        return ResponseEntity.ok(ApiResponse.ok(demands));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Demand>> getDemandById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(demandService.getDemandById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Demand>> createDemand(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody DemandDto dto) {
        Demand demand = demandService.createDemand(principal, dto);
        return ResponseEntity.ok(ApiResponse.ok(demand, "Demand created successfully"));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Demand>> updateDemand(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody DemandDto dto) {
        Demand demand = demandService.updateDemand(id, principal, dto);
        return ResponseEntity.ok(ApiResponse.ok(demand, "Demand updated successfully"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<String>> deleteDemand(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        demandService.deleteDemand(id, principal);
        return ResponseEntity.ok(ApiResponse.ok("Demand cancelled successfully"));
    }
}
