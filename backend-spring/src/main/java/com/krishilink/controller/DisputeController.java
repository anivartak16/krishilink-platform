package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.Dispute;
import com.krishilink.entity.Transaction;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.repository.DisputeRepository;
import com.krishilink.repository.TransactionRepository;
import com.krishilink.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeRepository disputeRepository;
    private final TransactionRepository transactionRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Dispute>>> getDisputes(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Dispute> disputes = disputeRepository.findByRaisedBy(principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok(disputes));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Dispute>> raiseDispute(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long txId = Long.valueOf(body.get("transactionId").toString());
        String reason = body.get("reason").toString();
        String description = body.get("description").toString();

        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        Dispute dispute = Dispute.builder()
                .transaction(tx)
                .raisedBy(principal.getUser())
                .against(principal.getUser().getId().equals(tx.getBuyer().getId()) ? tx.getSeller() : tx.getBuyer())
                .reason(reason)
                .description(description)
                .status("OPEN")
                .build();

        dispute = disputeRepository.save(dispute);
        return ResponseEntity.ok(ApiResponse.ok(dispute, "Dispute submitted"));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Dispute>> resolveDispute(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Dispute dispute = disputeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found"));

        if (body.containsKey("status")) dispute.setStatus(body.get("status"));
        if (body.containsKey("resolution")) dispute.setResolution(body.get("resolution"));

        dispute = disputeRepository.save(dispute);
        return ResponseEntity.ok(ApiResponse.ok(dispute, "Dispute updated"));
    }
}
