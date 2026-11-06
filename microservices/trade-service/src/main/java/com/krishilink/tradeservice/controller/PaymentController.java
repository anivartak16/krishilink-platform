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

        String method = body != null && body.get("paymentMethod") != null ?
                body.get("paymentMethod").toString() : "UPI_ESCROW";
        String remarks = body != null && body.get("remarks") != null ?
                body.get("remarks").toString() : "Payment deposited in KrishiLink Escrow account";
        boolean simulateFail = body != null && Boolean.TRUE.equals(body.get("simulateFailure"));

        String refId = "PAY-ESC-" + System.currentTimeMillis();
        payment.setPaymentMethod(method);
        payment.setAmount(tx.getAmount());
        payment.setReferenceId(refId);

        if (simulateFail) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setRemarks("Escrow deposit failed during bank authorization.");
            payment = paymentRepository.save(payment);
            return ResponseEntity.ok(ApiResponse.ok(Map.of("payment", payment, "transaction", tx), "Payment failed"));
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setRemarks(remarks);
        payment = paymentRepository.save(payment);

        if (tx.getStatus() == TransactionStatus.CREATED) {
            tx.setStatus(TransactionStatus.CONFIRMED);
            transactionRepository.save(tx);
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of("payment", payment, "transaction", tx), "Payment secured in escrow"));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<Payment>> createPayment(@RequestBody Map<String, Object> body) {
        Long txId = Long.valueOf(body.get("transactionId").toString());
        Transaction tx = transactionRepository.findById(txId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));

        Double amount = body.get("amount") != null ? Double.valueOf(body.get("amount").toString()) : tx.getAmount();
        String method = body.get("paymentMethod") != null ? body.get("paymentMethod").toString() : "Bank Transfer";

        Payment payment = paymentRepository.findByTransactionId(tx.getId()).orElse(
                Payment.builder().transactionId(tx.getId()).build()
        );
        payment.setAmount(amount);
        payment.setPaymentMethod(method);
        payment.setStatus(PaymentStatus.PROCESSING);
        payment.setReferenceId(body.get("referenceId") != null ? body.get("referenceId").toString() : "REF-" + System.currentTimeMillis());
        payment = paymentRepository.save(payment);

        return ResponseEntity.ok(ApiResponse.ok(payment, "Payment initiated"));
    }

    @PatchMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Payment>> updatePayment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Payment payment = paymentRepository.findById(id)
                .or(() -> paymentRepository.findByTransactionId(id))
                .orElseThrow(() -> new RuntimeException("Payment not found"));

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
