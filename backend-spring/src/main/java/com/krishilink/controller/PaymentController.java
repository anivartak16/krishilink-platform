package com.krishilink.controller;

import java.math.BigDecimal;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.Payment;
import com.krishilink.entity.Transaction;
import com.krishilink.entity.User;
import com.krishilink.enums.PaymentStatus;
import com.krishilink.enums.Role;
import com.krishilink.enums.TransactionStatus;
import com.krishilink.exception.BadRequestException;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.repository.PaymentRepository;
import com.krishilink.repository.TransactionRepository;
import com.krishilink.security.UserPrincipal;
import com.krishilink.outbox.OutboxPublisher;
import com.krishilink.outbox.TradeEvents;
import com.krishilink.service.IdempotencyService;
import com.krishilink.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentRepository paymentRepository;
    private final TransactionRepository transactionRepository;
    private final OutboxPublisher outboxPublisher;
    private final IdempotencyService idempotencyService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Payment>>> getPayments(
            @AuthenticationPrincipal UserPrincipal principal) {
        User user = principal.getUser();
        if (user.getRole() == Role.ADMIN) {
            return ResponseEntity.ok(ApiResponse.ok(paymentRepository.findAll()));
        }
        List<Payment> payments = transactionRepository.findByUser(user).stream()
                .map(paymentRepository::findByTransaction)
                .flatMap(java.util.Optional::stream)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(payments));
    }

    @GetMapping("/{transactionId}")
    @Transactional
    public ResponseEntity<ApiResponse<Map<String, Object>>> getByTransactionId(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal UserPrincipal principal) {
        Transaction tx = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        TransactionService.requireParticipantOrAdmin(tx, principal.getUser());

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
    public ResponseEntity<?> processPayment(
            @PathVariable Long transactionId,
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = IdempotencyService.HEADER, required = false) String idempotencyKey,
            @RequestBody(required = false) Map<String, Object> body) {
        User user = principal.getUser();
        Transaction tx = transactionRepository.findByIdForUpdate(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        requireBuyerOrAdmin(tx, user);

        String requestHash = idempotencyService.requestHash("POST /api/payments/" + transactionId + "/process", body);
        Optional<ResponseEntity<String>> replay = idempotencyService.findReplay(user, idempotencyKey, requestHash);
        if (replay.isPresent()) {
            return replay.get();
        }
        ResponseEntity<ApiResponse<Map<String, Object>>> response = settle(tx, body);
        idempotencyService.remember(user, idempotencyKey, requestHash, response);
        return response;
    }

    private ResponseEntity<ApiResponse<Map<String, Object>>> settle(Transaction tx, Map<String, Object> body) {
        Payment existing = paymentRepository.findByTransaction(tx).orElse(null);
        if (existing != null && (existing.getStatus() == PaymentStatus.PAID
                || existing.getStatus() == PaymentStatus.REFUNDED)) {
            return ResponseEntity.ok(ApiResponse.ok(
                    Map.of("payment", existing, "transaction", tx),
                    "Payment already processed"
            ));
        }
        if (tx.getStatus() != TransactionStatus.CREATED) {
            throw new BadRequestException("Payment is not allowed for a " + tx.getStatus() + " transaction");
        }

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
            outboxPublisher.publish("Transaction", tx.getId(), TradeEvents.PAYMENT_SECURED, Map.of(
                    "paymentId", payment.getId(),
                    "sellerId", tx.getSeller().getId(),
                    "amount", tx.getAmount().toPlainString(),
                    "commodity", tx.getLot() != null ? tx.getLot().getCommodity() : "order"));
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "payment", payment,
                "transaction", tx
        ), "Payment processed successfully"));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> createPayment(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestHeader(value = IdempotencyService.HEADER, required = false) String idempotencyKey,
            @RequestBody Map<String, Object> body) {
        if (body.get("transactionId") == null) {
            throw new BadRequestException("transactionId is required");
        }
        User user = principal.getUser();
        Long txId = Long.valueOf(body.get("transactionId").toString());
        Transaction tx = transactionRepository.findByIdForUpdate(txId)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found"));
        requireBuyerOrAdmin(tx, user);

        String requestHash = idempotencyService.requestHash("POST /api/payments", body);
        Optional<ResponseEntity<String>> replay = idempotencyService.findReplay(user, idempotencyKey, requestHash);
        if (replay.isPresent()) {
            return replay.get();
        }
        ResponseEntity<ApiResponse<Payment>> response = initiate(tx, body);
        idempotencyService.remember(user, idempotencyKey, requestHash, response);
        return response;
    }

    private ResponseEntity<ApiResponse<Payment>> initiate(Transaction tx, Map<String, Object> body) {

        Payment current = paymentRepository.findByTransaction(tx).orElse(null);
        if (current != null && current.getStatus() == PaymentStatus.PAID) {
            throw new BadRequestException("Payment already completed for this transaction");
        }

        BigDecimal amount = tx.getAmount();
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
            try {
                payment.setStatus(PaymentStatus.valueOf(body.get("status").toUpperCase().trim()));
            } catch (IllegalArgumentException e) {
                throw new BadRequestException("Invalid payment status");
            }
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

    private static void requireBuyerOrAdmin(Transaction tx, User user) {
        boolean buyer = tx.getBuyer() != null && tx.getBuyer().getId().equals(user.getId());
        if (!buyer && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Only the buyer can pay for this transaction");
        }
    }
}
