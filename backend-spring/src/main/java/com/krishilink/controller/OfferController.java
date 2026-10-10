package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.dto.OfferDto;
import com.krishilink.entity.Offer;
import com.krishilink.entity.Transaction;
import com.krishilink.security.UserPrincipal;
import com.krishilink.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferService offerService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Offer>>> getOffers(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(required = false) String status) {
        List<Offer> offers = offerService.getOffers(principal.getUser(), status);
        return ResponseEntity.ok(ApiResponse.ok(offers));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Offer>> getOfferById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(offerService.getOfferById(id)));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Offer>> createOffer(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody OfferDto dto) {
        Offer offer = offerService.createOffer(principal.getUser(), dto);
        return ResponseEntity.ok(ApiResponse.ok(offer, "Offer sent successfully"));
    }

    @PatchMapping("/{id}/accept")
    public ResponseEntity<ApiResponse<Transaction>> acceptOffer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Transaction tx = offerService.acceptOffer(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok(tx, "Offer accepted and transaction created"));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Offer>> rejectOffer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Offer offer = offerService.rejectOffer(id, principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok(offer, "Offer rejected"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Object>> updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if ("ACCEPTED".equalsIgnoreCase(status)) {
            Transaction tx = offerService.acceptOffer(id, principal.getUser());
            return ResponseEntity.ok(ApiResponse.ok(tx, "Offer accepted"));
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            Offer offer = offerService.rejectOffer(id, principal.getUser());
            return ResponseEntity.ok(ApiResponse.ok(offer, "Offer rejected"));
        }
        return ResponseEntity.ok(ApiResponse.ok(offerService.getOfferById(id)));
    }
}
