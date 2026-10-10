package com.krishilink.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final ObjectMapper objectMapper;

    public List<Transaction> getUserTransactions(User user) {
        if (user.getRole() == Role.ADMIN) {
            return transactionRepository.findAll();
        }
        return transactionRepository.findByUser(user);
    }

    public Transaction getTransactionById(Long id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
    }

    @Transactional
    public Transaction updateStatus(Long id, User user, String newStatus, String note) {
        Transaction tx = getTransactionById(id);

        TransactionStatus status;
        try {
            status = TransactionStatus.valueOf(newStatus.toUpperCase().trim());
        } catch (Exception e) {
            throw new BadRequestException("Invalid transaction status");
        }

        tx.setStatus(status);

        // Append to eventsJson
        List<Map<String, Object>> events = new ArrayList<>();
        if (tx.getEventsJson() != null && !tx.getEventsJson().isBlank()) {
            try {
                events = objectMapper.readValue(tx.getEventsJson(), new TypeReference<>() {});
            } catch (Exception ignored) {}
        }
        events.add(Map.of(
                "status", status.name(),
                "note", note != null ? note : "Status updated to " + status.name(),
                "at", LocalDateTime.now().toString()
        ));
        try {
            tx.setEventsJson(objectMapper.writeValueAsString(events));
        } catch (Exception ignored) {}

        if (status == TransactionStatus.COMPLETED) {
            paymentRepository.findByTransaction(tx).ifPresent(p -> {
                p.setStatus(PaymentStatus.PAID);
                p.setPaidAt(LocalDateTime.now());
                paymentRepository.save(p);
            });
        }

        return transactionRepository.save(tx);
    }
}
