package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.Payment;
import com.krishilink.entity.Transaction;
import com.krishilink.enums.PaymentStatus;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.repository.PaymentRepository;
import com.krishilink.repository.TransactionRepository;
import com.krishilink.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Payment>>> getPayments() {
        return ResponseEntity.ok(ApiResponse.ok(paymentRepository.findAll()));
    }

    @GetMapping("/{transactionId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getByTransactionId(@PathVariable Long transactionId) {
        Payment payment = paymentRepository.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for transaction"));
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "payment", payment,
                "transaction", payment.getTransaction()
        )));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Payment>> createPayment(@RequestBody Map<String, Object> body) {
        Long txId = Long.valueOf(body.get("transactionId").toString());
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        Double amount = body.get("amount") != null ? Double.valueOf(body.get("amount").toString()) : tx.getAmount();
        String method = body.get("paymentMethod") != null ? body.get("paymentMethod").toString() : "Bank Transfer";

        Payment payment = paymentRepository.findByTransaction(tx).orElse(
                Payment.builder().transaction(tx).build()
        );
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setReferenceId(body.get("referenceId") != null ? body.get("referenceId").toString() : "REF-" + System.currentTimeMillis());
        payment = paymentRepository.save(payment);

        return ResponseEntity.ok(ApiResponse.ok(payment, "Payment initiated"));
    }

    @PostMapping("/{id}/process")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> processPayment(
            @PathVariable Long id,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody(required = false) Map<String, Object> body) {
        Transaction tx = transactionRepository.findById(id).orElse(null);
        Payment payment;
        if (tx != null) {
            payment = paymentRepository.findByTransaction(tx)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment record not found for transaction"));
        } else {
            payment = paymentRepository.findById(id)
                    .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));
            tx = payment.getTransaction();
        }

        if (principal == null || principal.getUser() == null) {
            throw new ResourceNotFoundException("Authenticated user not found");
        }
        Long userId = principal.getUser().getId();
        if (!userId.equals(tx.getBuyer().getId()) && !userId.equals(tx.getSeller().getId())
                && principal.getUser().getRole() != com.krishilink.enums.Role.ADMIN) {
            throw new com.krishilink.exception.BadRequestException("You are not allowed to process this payment");
        }
        if (payment.getStatus() == PaymentStatus.PAID) {
            return paymentResult(payment, tx, "Payment already confirmed");
        }
        if (body != null && Boolean.parseBoolean(String.valueOf(body.get("simulateFailure")))) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setRemarks(body.get("remarks") != null ? String.valueOf(body.get("remarks")) : "Payment failed");
            paymentRepository.save(payment);
            throw new com.krishilink.exception.BadRequestException("Payment authorization failed");
        }

        payment.setPaymentMethod(body != null && body.get("paymentMethod") != null
                ? String.valueOf(body.get("paymentMethod")) : payment.getPaymentMethod());
        payment.setRemarks(body != null && body.get("remarks") != null
                ? String.valueOf(body.get("remarks")) : payment.getRemarks());
        payment.setReferenceId("KL-" + System.currentTimeMillis());
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        tx.setStatus(com.krishilink.enums.TransactionStatus.CONFIRMED);
        paymentRepository.save(payment);
        transactionRepository.save(tx);
        return paymentResult(payment, tx, "Payment confirmed and escrow secured");
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> paymentResult(
            Payment payment, Transaction transaction, String message) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "payment", payment,
                "transaction", transaction
        ), message));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<Payment>> updatePayment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Payment payment = paymentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (body.containsKey("status")) {
            payment.setStatus(PaymentStatus.valueOf(body.get("status").toUpperCase()));
            if (payment.getStatus() == PaymentStatus.PAID) {
                payment.setPaidAt(LocalDateTime.now());
            }
        }
        if (body.containsKey("remarks")) {
            payment.setRemarks(body.get("remarks"));
        }
        payment = paymentRepository.save(payment);

        return ResponseEntity.ok(ApiResponse.ok(payment, "Payment status updated"));
    }
}
