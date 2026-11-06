package com.krishilink.tradeservice.controller;

import com.krishilink.tradeservice.dto.ApiResponse;
import com.krishilink.tradeservice.entity.Payment;
import com.krishilink.tradeservice.entity.Transaction;
import com.krishilink.tradeservice.enums.PaymentStatus;
import com.krishilink.tradeservice.enums.TransactionStatus;
import com.krishilink.tradeservice.repository.PaymentRepository;
import com.krishilink.tradeservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

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
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> getByTransactionId(@PathVariable Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        Payment payment = paymentRepository.findByTransactionId(tx.getId()).orElseGet(() -> {
            Payment p = Payment.builder()
                    .transactionId(tx.getId())
                    .amount(tx.getAmount())
                    .status(PaymentStatus.PENDING)
                    .build();
            return paymentRepository.save(p);
        });

        return ResponseEntity.ok(ApiResponse.ok(Map.of("payment", payment, "transaction", tx)));
    }

    @PostMapping("/{transactionId}/process")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> processPayment(
            @PathVariable Long transactionId,
            @RequestBody(required = false) Map<String, Object> body) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        Payment payment = paymentRepository.findByTransactionId(tx.getId()).orElseGet(() ->
                Payment.builder().transactionId(tx.getId()).amount(tx.getAmount()).build());

        String refId = "PAY-" + System.currentTimeMillis();
        payment.setPaymentMethod(body != null && body.get("paymentMethod") != null ? body.get("paymentMethod").toString() : "UPI");
        payment.setAmount(tx.getAmount());
        payment.setReferenceId(refId);
        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setRemarks("Escrow settlement secured. Ref: " + refId);
        payment = paymentRepository.save(payment);

        tx.setStatus(TransactionStatus.CONFIRMED);
        transactionRepository.save(tx);

        return ResponseEntity.ok(ApiResponse.ok(Map.of("payment", payment, "transaction", tx), "Escrow Payment Secured"));
    }
}
