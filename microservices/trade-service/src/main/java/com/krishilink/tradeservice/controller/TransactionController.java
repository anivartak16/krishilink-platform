package com.krishilink.tradeservice.controller;

import com.krishilink.tradeservice.dto.ApiResponse;
import com.krishilink.tradeservice.entity.Transaction;
import com.krishilink.tradeservice.enums.TransactionStatus;
import com.krishilink.tradeservice.repository.TransactionRepository;
import com.krishilink.tradeservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionRepository transactionRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Transaction>>> getTransactions(@AuthenticationPrincipal UserPrincipal principal) {
        if ("ADMIN".equalsIgnoreCase(principal.getRole())) {
            return ResponseEntity.ok(ApiResponse.ok(transactionRepository.findAll()));
        }
        return ResponseEntity.ok(ApiResponse.ok(transactionRepository.findByBuyerIdOrSellerId(principal.getId(), principal.getId())));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Transaction>> getTransactionById(@PathVariable Long id) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        return ResponseEntity.ok(ApiResponse.ok(tx));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Transaction>> updateStatus(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Transaction tx = transactionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        String statusStr = body.getOrDefault("status", "COMPLETED");
        tx.setStatus(TransactionStatus.valueOf(statusStr.toUpperCase()));
        tx = transactionRepository.save(tx);
        return ResponseEntity.ok(ApiResponse.ok(tx, "Status updated to " + tx.getStatus()));
    }
}
