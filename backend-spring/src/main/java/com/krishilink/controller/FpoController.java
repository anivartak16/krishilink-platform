package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.*;
import com.krishilink.enums.LotStatus;
import com.krishilink.enums.Role;
import com.krishilink.enums.VerificationStatus;
import com.krishilink.exception.BadRequestException;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.repository.*;
import com.krishilink.security.UserPrincipal;
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
    private final FpoMembershipRepository membershipRepository;
    private final DemandRepository demandRepository;
    private final OfferRepository offerRepository;
    private final NotificationRepository notificationRepository;

    @GetMapping("/lots")
    public ResponseEntity<ApiResponse<List<Lot>>> getFpoLots(@AuthenticationPrincipal UserPrincipal principal) {
        User fpo = requireFpo(principal.getUser());
        List<Long> memberIds = membershipRepository.findByFpoOrderByCreatedAtDesc(fpo).stream()
                .map(m -> m.getFarmer().getId()).toList();
        List<Lot> lots = new ArrayList<>(lotRepository.findByOwner(fpo));
        if (!memberIds.isEmpty()) {
            lots.addAll(lotRepository.findAll().stream()
                    .filter(l -> l.getOwner() != null && memberIds.contains(l.getOwner().getId()))
                    .filter(l -> l.getStatus() == LotStatus.AVAILABLE || l.getStatus() == LotStatus.PARTIALLY_SOLD)
                    .toList());
        }
        return ResponseEntity.ok(ApiResponse.ok(lots));
    }

    @GetMapping("/farmers")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> searchFarmers(
            @RequestParam(defaultValue = "") String search) {
        List<User> farmers = search.isBlank() ? userRepository.findByRole(Role.FARMER)
                : userRepository.searchByRole(Role.FARMER, search.trim());
        return ResponseEntity.ok(ApiResponse.ok(farmers.stream().map(this::userView).toList()));
    }

    @GetMapping("/members")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getMembers(
            @AuthenticationPrincipal UserPrincipal principal) {
        User fpo = requireFpo(principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok(membershipRepository.findByFpoOrderByCreatedAtDesc(fpo).stream()
                .map(m -> {
                    Map<String, Object> view = userView(m.getFarmer());
                    view.put("memberId", m.getId());
                    view.put("farmerId", m.getFarmer().getId());
                    view.put("_id", m.getId().toString());
                    view.put("ekycVerified", m.getEkycVerified());
                    return view;
                }).toList()));
    }

    @PostMapping("/members")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> addMember(
            @AuthenticationPrincipal UserPrincipal principal, @RequestBody Map<String, Object> body) {
        User fpo = requireFpo(principal.getUser());
        User farmer = userRepository.findById(id(body.get("farmerId"), "farmerId"))
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found"));
        if (farmer.getRole() != Role.FARMER) throw new BadRequestException("Only farmers can join an FPO");
        if (membershipRepository.existsByFpoAndFarmer(fpo, farmer))
            throw new BadRequestException("Farmer is already an FPO member");
        membershipRepository.save(FpoMembership.builder().fpo(fpo).farmer(farmer).build());
        return ResponseEntity.ok(ApiResponse.ok(userView(farmer), "Farmer added to FPO"));
    }

    @DeleteMapping("/members/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> removeMember(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        User fpo = requireFpo(principal.getUser());
        FpoMembership membership = findMembership(fpo, id);
        if (!membership.getFpo().getId().equals(fpo.getId())) throw new BadRequestException("Member is not in your FPO");
        Map<String, Object> result = userView(membership.getFarmer());
        membershipRepository.delete(membership);
        return ResponseEntity.ok(ApiResponse.ok(result, "Farmer removed from FPO"));
    }

    @GetMapping("/members/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMember(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        User fpo = requireFpo(principal.getUser());
        FpoMembership membership = findMembership(fpo, id);
        Map<String, Object> result = userView(membership.getFarmer());
        result.put("memberId", membership.getId());
        result.put("ekycVerified", membership.getEkycVerified());
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PostMapping("/members/{id}/verify-ekyc")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyMember(
            @AuthenticationPrincipal UserPrincipal principal, @PathVariable Long id) {
        User fpo = requireFpo(principal.getUser());
        FpoMembership membership = findMembership(fpo, id);
        if (!membership.getFpo().getId().equals(fpo.getId())) throw new BadRequestException("Member is not in your FPO");
        User farmer = membership.getFarmer();
        membership.setEkycVerified(true);
        farmer.setKycVerified(true);
        farmer.setEkycStatus("VERIFIED");
        farmer.setEkycVerifiedAt(LocalDateTime.now());
        farmer.setVerification(VerificationStatus.VERIFIED);
        farmer.setVerificationBadge("FPO_FIELD_VERIFIED_FARMER");
        membershipRepository.save(membership);
        userRepository.save(farmer);
        return ResponseEntity.ok(ApiResponse.ok(userView(farmer), "Member e-KYC verified"));
    }

    @PostMapping("/aggregate")
    @Transactional
    public ResponseEntity<ApiResponse<Lot>> aggregate(
            @AuthenticationPrincipal UserPrincipal principal, @RequestBody Map<String, Object> body) {
        User fpo = requireFpo(principal.getUser());
        Object rawIds = body.get("lotIds");
        if (!(rawIds instanceof List<?> ids) || ids.size() < 2)
            throw new BadRequestException("Select at least two lots to aggregate");
        List<Long> memberIds = membershipRepository.findByFpoOrderByCreatedAtDesc(fpo).stream()
                .map(m -> m.getFarmer().getId()).toList();
        List<Lot> lots = new ArrayList<>();
        for (Object raw : ids) {
            Lot lot = lotRepository.findById(id(raw, "lotIds"))
                    .orElseThrow(() -> new ResourceNotFoundException("Lot not found"));
            boolean allowed = lot.getOwner().getId().equals(fpo.getId()) || memberIds.contains(lot.getOwner().getId());
            if (!allowed || (lot.getStatus() != LotStatus.AVAILABLE && lot.getStatus() != LotStatus.PARTIALLY_SOLD))
                throw new BadRequestException("Selected lot is not available to this FPO");
            lots.add(lot);
        }
        String commodity = lots.get(0).getCommodity();
        if (lots.stream().anyMatch(l -> !commodity.equalsIgnoreCase(l.getCommodity())))
            throw new BadRequestException("All aggregated lots must contain the same commodity");
        double quantity = lots.stream().mapToDouble(l -> l.getRemainingQuantity() == null ? 0 : l.getRemainingQuantity()).sum();
        if (quantity <= 0) throw new BadRequestException("Selected lots have no remaining quantity");
        double expectedPrice = body.get("expectedPrice") == null ? lots.get(0).getExpectedPrice()
                : number(body.get("expectedPrice"), "expectedPrice");
        Lot aggregate = Lot.builder().owner(fpo).ownerType("FPO").commodity(commodity)
                .quantity(quantity).remainingQuantity(quantity).unit(lots.get(0).getUnit())
                .location(fpo.getLocation()).latitude(fpo.getLatitude()).longitude(fpo.getLongitude())
                .expectedPrice(expectedPrice).quality(lots.get(0).getQuality())
                .availableUntil(LocalDate.now().plusDays(30)).farmingType(lots.get(0).getFarmingType())
                .status(LotStatus.AVAILABLE).build();
        for (Lot lot : lots) {
            lot.setRemainingQuantity(0.0);
            lot.setStatus(LotStatus.SOLD);
        }
        lotRepository.saveAll(lots);
        return ResponseEntity.ok(ApiResponse.ok(lotRepository.save(aggregate), "Aggregate lot created"));
    }

    @PostMapping("/connect")
    @Transactional
    public ResponseEntity<ApiResponse<Offer>> connect(
            @AuthenticationPrincipal UserPrincipal principal, @RequestBody Map<String, Object> body) {
        User seller = principal.getUser();
        Demand demand = demandRepository.findById(id(body.get("demandId"), "demandId"))
                .orElseThrow(() -> new ResourceNotFoundException("Demand not found"));
        double quantity = number(body.get("proposedQuantity"), "proposedQuantity");
        double price = number(body.get("proposedPrice"), "proposedPrice");
        Lot lot = lotRepository.findByOwner(seller).stream()
                .filter(l -> l.getCommodity().equalsIgnoreCase(demand.getCommodity()))
                .filter(l -> l.getStatus() == LotStatus.AVAILABLE || l.getStatus() == LotStatus.PARTIALLY_SOLD)
                .filter(l -> l.getRemainingQuantity() != null && l.getRemainingQuantity() >= quantity)
                .findFirst().orElseThrow(() -> new BadRequestException("No matching available lot for this proposal"));
        Offer offer = offerRepository.save(Offer.builder().lot(lot).buyer(demand.getBuyer()).quantity(quantity)
                .pricePerUnit(price).totalAmount(quantity * price)
                .message(body.get("message") == null ? null : body.get("message").toString())
                .validUntil(demand.getDeadline()).build());
        notificationRepository.save(Notification.builder().user(demand.getBuyer()).type("SUPPLY_PROPOSAL")
                .message(seller.getName() + " sent a supply proposal for " + quantity + " kg " + lot.getCommodity())
                .read(false).build());
        return ResponseEntity.ok(ApiResponse.ok(offer, "Supply proposal sent"));
    }

    private static User requireFpo(User user) {
        if (user == null || user.getRole() != Role.FPO) throw new BadRequestException("FPO access is required");
        return user;
    }
    private FpoMembership findMembership(User fpo, Long id) {
        return membershipRepository.findById(id)
                .filter(m -> m.getFpo().getId().equals(fpo.getId()))
                .or(() -> membershipRepository.findByFpoAndFarmerId(fpo, id))
                .orElseThrow(() -> new ResourceNotFoundException("FPO member not found"));
    }
    private Map<String, Object> userView(User user) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", user.getId()); map.put("_id", user.getId().toString());
        map.put("name", user.getName()); map.put("email", user.getEmail()); map.put("role", user.getRole().name());
        map.put("phone", user.getPhone()); map.put("location", user.getLocation()); map.put("primaryCrop", user.getPrimaryCrop());
        map.put("verification", user.getVerification().name()); map.put("verificationBadge", user.getVerificationBadge());
        map.put("kycVerified", user.getKycVerified()); map.put("ekycStatus", user.getEkycStatus());
        return map;
    }
    private static Long id(Object value, String field) {
        if (value == null) throw new BadRequestException(field + " is required");
        try { return Long.valueOf(value.toString()); }
        catch (NumberFormatException ex) { throw new BadRequestException(field + " must be numeric"); }
    }
    private static double number(Object value, String field) {
        if (value == null) throw new BadRequestException(field + " is required");
        try { return Double.parseDouble(value.toString()); }
        catch (NumberFormatException ex) { throw new BadRequestException(field + " must be numeric"); }
    }
}
