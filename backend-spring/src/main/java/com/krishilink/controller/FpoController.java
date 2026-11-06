package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.*;
import com.krishilink.enums.DemandStatus;
import com.krishilink.enums.LotStatus;
import com.krishilink.enums.OfferStatus;
import com.krishilink.enums.Role;
import com.krishilink.enums.VerificationStatus;
import com.krishilink.exception.BadRequestException;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.repository.*;
import com.krishilink.security.UserPrincipal;
import com.krishilink.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@RestController
@RequestMapping("/api/fpo")
@RequiredArgsConstructor
public class FpoController {

    private final LotRepository lotRepository;
    private final UserRepository userRepository;
    private final FpoMembershipRepository fpoMembershipRepository;
    private final DemandRepository demandRepository;
    private final OfferRepository offerRepository;
    private final NotificationRepository notificationRepository;
    private final AuthService authService;

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFpoMembers(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<FpoMembership> memberships = fpoMembershipRepository.findByFpo(principal.getUser());
        List<Map<String, Object>> members = memberships.stream()
                .map(m -> authService.sanitizeUser(m.getFarmer()))
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(members));
    }

    @GetMapping("/farmers")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getFpoFarmers(
            @RequestParam(required = false) String search) {
        String s = search != null ? search.toLowerCase().trim() : "";
        List<Map<String, Object>> farmers = userRepository.findAll().stream()
                .filter(u -> u.getRole() == Role.FARMER)
                .filter(u -> s.isEmpty() ||
                        (u.getName() != null && u.getName().toLowerCase().contains(s)) ||
                        (u.getEmail() != null && u.getEmail().toLowerCase().contains(s)) ||
                        (u.getLocation() != null && u.getLocation().toLowerCase().contains(s)) ||
                        (u.getDistrict() != null && u.getDistrict().toLowerCase().contains(s)))
                .limit(30)
                .map(authService::sanitizeUser)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(farmers));
    }

    @PostMapping("/members")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> addMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long farmerId = Long.valueOf(body.get("farmerId").toString());
        User farmer = userRepository.findById(farmerId)
                .filter(u -> u.getRole() == Role.FARMER)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found"));

        if (!fpoMembershipRepository.existsByFpoAndFarmer(principal.getUser(), farmer)) {
            FpoMembership m = FpoMembership.builder()
                    .fpo(principal.getUser())
                    .farmer(farmer)
                    .build();
            fpoMembershipRepository.save(m);
        }

        return ResponseEntity.ok(ApiResponse.ok(authService.sanitizeUser(farmer), "Farmer added to FPO"));
    }

    @DeleteMapping("/members/{farmerId}")
    @Transactional
    public ResponseEntity<ApiResponse<String>> removeMember(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long farmerId) {
        User farmer = userRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found"));
        fpoMembershipRepository.deleteByFpoAndFarmer(principal.getUser(), farmer);
        return ResponseEntity.ok(ApiResponse.ok("Farmer removed from FPO"));
    }

    @PostMapping("/members/{farmerId}/verify-ekyc")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyMemberFarmer(
            @PathVariable Long farmerId) {
        User farmer = userRepository.findById(farmerId)
                .filter(u -> u.getRole() == Role.FARMER)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found"));

        farmer.setVerification(VerificationStatus.VERIFIED);
        farmer.setVerificationBadge("EKYC_VERIFIED_FARMER");
        farmer.setEkycStatus("VERIFIED");
        farmer.setEkycType("FPO_FIELD_VERIFICATION");
        farmer.setEkycIdNumber("FPO-VER-" + farmer.getId());
        farmer.setEkycVerifiedAt(LocalDateTime.now());
        farmer = userRepository.save(farmer);

        return ResponseEntity.ok(ApiResponse.ok(authService.sanitizeUser(farmer), "Farmer e-KYC certified successfully by FPO"));
    }

    @GetMapping("/lots")
    public ResponseEntity<ApiResponse<List<Lot>>> getFpoLots(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<FpoMembership> memberships = fpoMembershipRepository.findByFpo(principal.getUser());
        List<User> owners = new ArrayList<>();
        owners.add(principal.getUser());
        memberships.forEach(m -> owners.add(m.getFarmer()));

        List<Lot> lots = lotRepository.findAll().stream()
                .filter(l -> owners.contains(l.getOwner()))
                .filter(l -> l.getStatus() == LotStatus.AVAILABLE || l.getStatus() == LotStatus.PARTIALLY_SOLD)
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(lots));
    }

    @PostMapping("/aggregate")
    @Transactional
    public ResponseEntity<ApiResponse<Lot>> aggregateLots(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        List<?> rawIds = (List<?>) body.get("lotIds");
        if (rawIds == null || rawIds.isEmpty()) {
            throw new BadRequestException("Select at least two farmer lots to aggregate");
        }

        List<Long> lotIds = rawIds.stream().map(o -> Long.valueOf(o.toString())).toList();
        List<Lot> source = lotRepository.findAllById(lotIds);

        if (source.isEmpty()) {
            throw new BadRequestException("No valid lots found for aggregation");
        }

        String commodity = source.get(0).getCommodity();
        if (source.stream().anyMatch(l -> !l.getCommodity().equalsIgnoreCase(commodity))) {
            throw new BadRequestException("Only one commodity can be aggregated at a time");
        }

        double totalQty = source.stream().mapToDouble(l -> l.getRemainingQuantity() != null ? l.getRemainingQuantity() : l.getQuantity()).sum();
        double expectedPrice = body.get("expectedPrice") != null && Double.parseDouble(body.get("expectedPrice").toString()) > 0 ?
                Double.parseDouble(body.get("expectedPrice").toString()) :
                source.stream().mapToDouble(Lot::getExpectedPrice).average().orElse(25.0);

        Lot aggregated = Lot.builder()
                .owner(principal.getUser())
                .ownerType("FPO")
                .commodity(commodity)
                .quantity(totalQty)
                .remainingQuantity(totalQty)
                .expectedPrice(expectedPrice)
                .unit(source.get(0).getUnit())
                .location(principal.getUser().getLocation())
                .latitude(principal.getUser().getLatitude())
                .longitude(principal.getUser().getLongitude())
                .quality(source.get(0).getQuality())
                .status(LotStatus.AVAILABLE)
                .availableUntil(LocalDate.now().plusDays(14))
                .build();
        aggregated = lotRepository.save(aggregated);

        for (Lot s : source) {
            s.setStatus(LotStatus.CANCELLED);
            lotRepository.save(s);
        }

        return ResponseEntity.ok(ApiResponse.ok(aggregated, "FPO aggregated lot created successfully"));
    }

    @GetMapping("/matches")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFpoMatches(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Lot> lots = getFpoLots(principal).getBody().getData();
        List<Demand> demands = demandRepository.findByStatus(DemandStatus.ACTIVE);

        List<Map<String, Object>> matches = new ArrayList<>();
        for (Demand demand : demands) {
            for (Lot lot : lots) {
                if (lot.getCommodity().equalsIgnoreCase(demand.getCommodity())) {
                    matches.add(Map.of(
                            "demand", demand,
                            "lot", lot,
                            "matchScore", 85,
                            "isFpoAggregate", lot.getOwner().getId().equals(principal.getUser().getId())
                    ));
                }
            }
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "fpoLotsCount", lots.size(),
                "activeDemandsCount", demands.size(),
                "matches", matches,
                "demands", demands,
                "fpoLots", lots
        )));
    }

    @PostMapping("/connect")
    @Transactional
    public ResponseEntity<ApiResponse<Offer>> connectFpoWithBuyer(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long demandId = Long.valueOf(body.get("demandId").toString());
        Demand demand = demandRepository.findById(demandId)
                .orElseThrow(() -> new ResourceNotFoundException("Demand not found"));

        Long lotId = body.get("lotId") != null ? Long.valueOf(body.get("lotId").toString()) : null;
        Lot lot = lotId != null ? lotRepository.findById(lotId).orElse(null) : null;

        double price = body.get("proposedPrice") != null ?
                Double.parseDouble(body.get("proposedPrice").toString()) : demand.getMaxPrice();
        double qty = body.get("proposedQuantity") != null ?
                Double.parseDouble(body.get("proposedQuantity").toString()) : demand.getRequiredQuantity();

        Offer offer = Offer.builder()
                .lot(lot)
                .buyer(demand.getBuyer())
                .quantity(qty)
                .pricePerUnit(price)
                .totalAmount(qty * price)
                .message(body.get("message") != null ? body.get("message").toString() :
                        "FPO Supply Proposal from " + principal.getUser().getName())
                .status(OfferStatus.PENDING)
                .validUntil(LocalDate.now().plusDays(7))
                .build();
        offer = offerRepository.save(offer);

        Notification notif = Notification.builder()
                .user(demand.getBuyer())
                .type("NEW_OFFER")
                .message("Direct FPO Supply Proposal from " + principal.getUser().getName() + " for " + demand.getCommodity())
                .build();
        notificationRepository.save(notif);

        return ResponseEntity.ok(ApiResponse.ok(offer, "Proposal sent to buyer successfully"));
    }
}
