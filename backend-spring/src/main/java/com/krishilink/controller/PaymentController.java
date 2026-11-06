package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.Notification;
import com.krishilink.entity.Payment;
import com.krishilink.entity.Transaction;
import com.krishilink.enums.PaymentStatus;
import com.krishilink.enums.TransactionStatus;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.repository.NotificationRepository;
import com.krishilink.repository.PaymentRepository;
import com.krishilink.repository.TransactionRepository;
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
    private final NotificationRepository notificationRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Payment>>> getPayments() {
        return ResponseEntity.ok(ApiResponse.ok(paymentRepository.findAll()));
    }

    @GetMapping("/{transactionId}")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> getByTransactionId(@PathVariable Long transactionId) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        Payment payment = paymentRepository.findByTransaction(tx).orElseGet(() -> {
            Payment p = Payment.builder()
                    .transaction(tx)
                    .amount(tx.getAmount())
                    .status(PaymentStatus.PENDING)
                    .build();
            return paymentRepository.save(p);
        });

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "payment", payment,
                "transaction", tx
        )));
    }

    @PostMapping("/{transactionId}/process")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> processPayment(
            @PathVariable Long transactionId,
            @RequestBody(required = false) Map<String, Object> body) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));

        String method = body != null && body.get("paymentMethod") != null ?
                body.get("paymentMethod").toString() : "UPI";
        String remarks = body != null && body.get("remarks") != null ?
                body.get("remarks").toString() : "Escrow settlement secured";
        boolean simulateFail = body != null && Boolean.TRUE.equals(body.get("simulateFailure"));

        Payment payment = paymentRepository.findByTransaction(tx).orElseGet(() ->
                Payment.builder().transaction(tx).amount(tx.getAmount()).build()
        );

        String refId = "PAY-" + System.currentTimeMillis() + "-" + (1000 + (int) (Math.random() * 9000));
        payment.setPaymentMethod(method);
        payment.setAmount(tx.getAmount());
        payment.setReferenceId(refId);

        if (simulateFail) {
            payment.setStatus(PaymentStatus.FAILED);
            payment.setRemarks("Payment failed during processing simulation");
            payment = paymentRepository.save(payment);
            return ResponseEntity.ok(ApiResponse.ok(Map.of(
                    "payment", payment,
                    "transaction", tx
            ), "Payment failed"));
        }

        payment.setStatus(PaymentStatus.PAID);
        payment.setPaidAt(LocalDateTime.now());
        payment.setRemarks(remarks + " Ref: " + refId);
        payment = paymentRepository.save(payment);

        if (tx.getStatus() == TransactionStatus.CREATED) {
            tx.setStatus(TransactionStatus.CONFIRMED);
            transactionRepository.save(tx);
        }

        if (tx.getSeller() != null) {
            Notification notif = Notification.builder()
                    .user(tx.getSeller())
                    .type("PAYMENT_RECEIVED")
                    .message("₹" + tx.getAmount() + " secured in KrishiLink Escrow for " +
                            (tx.getLot() != null ? tx.getLot().getCommodity() : "order") + ". You can now arrange dispatch.")
                    .build();
            notificationRepository.save(notif);
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "payment", payment,
                "transaction", tx
        ), "Payment processed successfully"));
    }

    @PostMapping
    @Transactional
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

    @PatchMapping("/{id}")
    @Transactional
    public ResponseEntity<ApiResponse<Payment>> updatePayment(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        Payment payment = paymentRepository.findById(id)
                .or(() -> paymentRepository.findByTransactionId(id))
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
