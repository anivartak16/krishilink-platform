package com.krishilink.userservice.controller;

import com.krishilink.userservice.client.LotClient;
import com.krishilink.userservice.dto.ApiResponse;
import com.krishilink.userservice.entity.FpoMembership;
import com.krishilink.userservice.entity.User;
import com.krishilink.userservice.enums.Role;
import com.krishilink.userservice.enums.VerificationStatus;
import com.krishilink.userservice.repository.FpoMembershipRepository;
import com.krishilink.userservice.repository.UserRepository;
import com.krishilink.userservice.security.UserPrincipal;
import com.krishilink.userservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/fpo")
@RequiredArgsConstructor
public class FpoController {

    private final FpoMembershipRepository fpoMembershipRepository;
    private final UserRepository userRepository;
    private final AuthService authService;
    private final LotClient lotClient;

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMembers(@AuthenticationPrincipal UserPrincipal principal) {
        List<FpoMembership> list = fpoMembershipRepository.findByFpo(principal.getUser());
        List<Map<String, Object>> result = list.stream()
                .map(m -> authService.sanitizeUser(m.getFarmer()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @GetMapping("/farmers")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchFarmers(
            @RequestParam(required = false, defaultValue = "") String search) {
        List<User> farmers;
        if (search.isBlank()) {
            farmers = userRepository.findByRole(Role.FARMER);
        } else {
            farmers = userRepository.findByRoleAndNameContainingIgnoreCaseOrRoleAndLocationContainingIgnoreCase(
                    Role.FARMER, search, Role.FARMER, search);
        }
        return ResponseEntity.ok(ApiResponse.ok(farmers.stream().map(authService::sanitizeUser).toList()));
    }

    @PostMapping("/members")
    public ResponseEntity<ApiResponse<Map<String, Object>>> addMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long farmerId = Long.valueOf(body.get("farmerId").toString());
        User farmer = userRepository.findById(farmerId)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));

        FpoMembership membership = fpoMembershipRepository.findByFpoAndFarmer(principal.getUser(), farmer)
                .orElseGet(() -> FpoMembership.builder()
                        .fpo(principal.getUser())
                        .farmer(farmer)
                        .status("ACTIVE")
                        .build());

        membership = fpoMembershipRepository.save(membership);
        return ResponseEntity.ok(ApiResponse.ok(authService.sanitizeUser(farmer), "Farmer joined FPO cluster"));
    }

    @DeleteMapping("/members/{farmerId}")
    @Transactional
    public ResponseEntity<ApiResponse<Void>> removeMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long farmerId) {
        User farmer = userRepository.findById(farmerId)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));
        fpoMembershipRepository.deleteByFpoAndFarmer(principal.getUser(), farmer);
        return ResponseEntity.ok(ApiResponse.ok(null, "Farmer removed from FPO"));
    }

    @PostMapping("/members/{farmerId}/verify-ekyc")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyMemberEkyc(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long farmerId) {
        User farmer = userRepository.findById(farmerId)
                .orElseThrow(() -> new RuntimeException("Farmer not found"));
        farmer.setVerification(VerificationStatus.VERIFIED);
        farmer.setVerificationBadge("FPO_VERIFIED_FARMER");
        farmer.setKycVerified(true);
        farmer.setEkycStatus("VERIFIED");
        farmer = userRepository.save(farmer);
        return ResponseEntity.ok(ApiResponse.ok(authService.sanitizeUser(farmer), "Farmer verified by FPO"));
    }

    @PostMapping("/connect")
    public ResponseEntity<ApiResponse<Map<String, Object>>> connectWithFpo(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("connected", true), "Connection request sent"));
    }

    @GetMapping("/lots")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFpoLots(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<FpoMembership> memberships = fpoMembershipRepository.findByFpo(principal.getUser());
        List<Long> ownerIds = new ArrayList<>();
        ownerIds.add(principal.getId());
        memberships.forEach(m -> ownerIds.add(m.getFarmer().getId()));

        try {
            ApiResponse<List<Map<String, Object>>> lotRes = lotClient.getLotsByOwners(ownerIds);
            if (lotRes != null && lotRes.getData() != null) {
                return ResponseEntity.ok(lotRes);
            }
        } catch (Exception ignored) {}

        return ResponseEntity.ok(ApiResponse.ok(List.of()));
    }

    @PostMapping("/aggregate")
    public ResponseEntity<ApiResponse<Map<String, Object>>> aggregateLots(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> payload = new HashMap<>(body);
        payload.put("fpoId", principal.getId());
        payload.put("fpoName", principal.getUser().getName());
        payload.put("location", principal.getUser().getLocation());
        payload.put("latitude", principal.getUser().getLatitude());
        payload.put("longitude", principal.getUser().getLongitude());

        ApiResponse<Map<String, Object>> response = lotClient.aggregateLots(payload);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/matches")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFpoMatches(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(List.of()));
    }
}
