package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.Dispute;
import com.krishilink.entity.Transaction;
import com.krishilink.entity.User;
import com.krishilink.enums.Role;
import com.krishilink.exception.BadRequestException;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.repository.DisputeRepository;
import com.krishilink.repository.TransactionRepository;
import com.krishilink.security.UserPrincipal;
import com.krishilink.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/disputes")
@RequiredArgsConstructor
public class DisputeController {

    private final DisputeRepository disputeRepository;
    private final TransactionRepository transactionRepository;

    private static final Set<String> DISPUTE_STATUSES = Set.of("OPEN", "UNDER_REVIEW", "RESOLVED", "REJECTED");

    @GetMapping
    public ResponseEntity<ApiResponse<List<Dispute>>> getDisputes(
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();
        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.ok(ApiResponse.ok(disputeRepository.findAll()));
        }
        List<Dispute> disputes = new ArrayList<>(disputeRepository.findByRaisedBy(user));
        disputes.addAll(disputeRepository.findByAgainst(user));
        return ResponseEntity.ok(ApiResponse.ok(disputes));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Dispute>> raiseDispute(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        if (body.get("transactionId") == null || body.get("reason") == null) {
            throw new BadRequestException("transactionId and reason are required");
        }
        Long txId = Long.valueOf(body.get("transactionId").toString());
        String reason = body.get("reason").toString();
        String description = body.get("description") != null ? body.get("description").toString() : "";

        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        if (!TransactionService.isParticipant(tx, principal.getUser())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Only the buyer or seller can raise a dispute on this transaction");
        }

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

        if (body.containsKey("status")) {
            String status = body.get("status") == null ? "" : body.get("status").toUpperCase().trim();
            if (!DISPUTE_STATUSES.contains(status)) {
                throw new BadRequestException("Invalid dispute status");
            }
            dispute.setStatus(status);
        }
        if (body.containsKey("resolution")) dispute.setResolution(body.get("resolution"));

        dispute = disputeRepository.save(dispute);
        return ResponseEntity.ok(ApiResponse.ok(dispute, "Dispute updated"));
    }
}
