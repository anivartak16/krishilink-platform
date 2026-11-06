package com.krishilink.tradeservice.controller;

import com.krishilink.tradeservice.client.LotClient;
import com.krishilink.tradeservice.client.UserClient;
import com.krishilink.tradeservice.dto.ApiResponse;
import com.krishilink.tradeservice.dto.OfferDto;
import com.krishilink.tradeservice.entity.Offer;
import com.krishilink.tradeservice.entity.Transaction;
import com.krishilink.tradeservice.enums.OfferStatus;
import com.krishilink.tradeservice.enums.TransactionStatus;
import com.krishilink.tradeservice.repository.OfferRepository;
import com.krishilink.tradeservice.repository.TransactionRepository;
import com.krishilink.tradeservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/offers")
@RequiredArgsConstructor
public class OfferController {

    private final OfferRepository offerRepository;
    private final TransactionRepository transactionRepository;
    private final LotClient lotClient;
    private final UserClient userClient;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Offer>>> getOffers(@AuthenticationPrincipal UserPrincipal principal) {
        if ("ADMIN".equalsIgnoreCase(principal.getRole())) {
            return ResponseEntity.ok(ApiResponse.ok(offerRepository.findAll()));
        }
        return ResponseEntity.ok(ApiResponse.ok(offerRepository.findByBuyerIdOrSellerId(principal.getId(), principal.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Offer>> getOfferById(@PathVariable Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offer not found with id: " + id));
        return ResponseEntity.ok(ApiResponse.ok(offer));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Offer>> createOffer(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody OfferDto dto) {
        String commodity = "Produce";
        Long sellerId = 2L;
        String sellerName = "Ramesh Patel";
        try {
            ApiResponse<Map<String, Object>> lotRes = lotClient.getLotById(dto.getLotId());
            if (lotRes != null && lotRes.getData() != null) {
                commodity = (String) lotRes.getData().get("commodity");
                Map<?, ?> owner = (Map<?, ?>) lotRes.getData().get("owner");
                if (owner != null && owner.get("id") != null) {
                    sellerId = Long.valueOf(owner.get("id").toString());
                    sellerName = (String) owner.get("name");
                }
            }
        } catch (Exception ignored) {}

        String buyerName = "Buyer";
        String buyerPhone = "";
        try {
            ApiResponse<Map<String, Object>> u = userClient.getUserById(principal.getId());
            if (u != null && u.getData() != null) {
                buyerName = (String) u.getData().get("name");
                buyerPhone = (String) u.getData().get("phone");
            }
        } catch (Exception ignored) {}

        Offer offer = Offer.builder()
                .lotId(dto.getLotId())
                .commodity(commodity)
                .buyerId(principal.getId())
                .buyerName(buyerName)
                .buyerPhone(buyerPhone)
                .sellerId(sellerId)
                .sellerName(sellerName)
                .quantity(dto.getQuantity())
                .pricePerUnit(dto.getPricePerUnit())
                .totalAmount(dto.getQuantity() * dto.getPricePerUnit())
                .message(dto.getMessage())
                .status(OfferStatus.PENDING)
                .build();

        offer = offerRepository.save(offer);
        return ResponseEntity.ok(ApiResponse.ok(offer, "Offer sent successfully"));
    }

    @PatchMapping("/{id}/accept")
    @Transactional
    public ResponseEntity<ApiResponse<Offer>> acceptOffer(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offer not found"));

        offer.setStatus(OfferStatus.ACCEPTED);
        offer = offerRepository.save(offer);

        Transaction tx = Transaction.builder()
                .offerId(offer.getId())
                .lotId(offer.getLotId())
                .commodity(offer.getCommodity())
                .buyerId(offer.getBuyerId())
                .buyerName(offer.getBuyerName())
                .sellerId(offer.getSellerId())
                .sellerName(offer.getSellerName())
                .quantity(offer.getQuantity())
                .amount(offer.getTotalAmount())
                .status(TransactionStatus.CREATED)
                .build();
        transactionRepository.save(tx);

        return ResponseEntity.ok(ApiResponse.ok(offer, "Offer accepted! Transaction initiated."));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<ApiResponse<Offer>> rejectOffer(@PathVariable Long id) {
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offer not found"));
        offer.setStatus(OfferStatus.REJECTED);
        offer = offerRepository.save(offer);
        return ResponseEntity.ok(ApiResponse.ok(offer, "Offer rejected"));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Object>> updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        if ("ACCEPTED".equalsIgnoreCase(status)) {
            ApiResponse<Offer> res = acceptOffer(id, principal).getBody();
            return ResponseEntity.ok(ApiResponse.ok(res != null ? res.getData() : null));
        } else if ("REJECTED".equalsIgnoreCase(status)) {
            ApiResponse<Offer> res = rejectOffer(id).getBody();
            return ResponseEntity.ok(ApiResponse.ok(res != null ? res.getData() : null));
        }
        Offer offer = offerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Offer not found"));
        return ResponseEntity.ok(ApiResponse.ok(offer));
    }
}
