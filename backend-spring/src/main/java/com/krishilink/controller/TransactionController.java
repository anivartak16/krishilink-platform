package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.Transaction;
import com.krishilink.security.UserPrincipal;
import com.krishilink.service.TransactionService;
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

    private final TransactionService transactionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Transaction>>> getTransactions(
            @AuthenticationPrincipal UserPrincipal principal) {
        List<Transaction> list = transactionService.getUserTransactions(principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Transaction>> getTransactionById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(transactionService.getTransactionById(id)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<Transaction>> updateStatus(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, String> body) {
        String status = body.get("status");
        String note = body.get("note");
        Transaction tx = transactionService.updateStatus(id, principal.getUser(), status, note);
        return ResponseEntity.ok(ApiResponse.ok(tx, "Transaction status updated"));
    }
}
