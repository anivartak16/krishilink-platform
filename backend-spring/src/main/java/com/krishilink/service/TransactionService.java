package com.krishilink.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishilink.entity.Lot;
import com.krishilink.entity.Offer;
import com.krishilink.entity.Payment;
import com.krishilink.entity.Transaction;
import com.krishilink.entity.User;
import com.krishilink.enums.LotStatus;
import com.krishilink.enums.OfferStatus;
import com.krishilink.enums.PaymentStatus;
import com.krishilink.enums.Role;
import com.krishilink.enums.TransactionStatus;
import com.krishilink.exception.BadRequestException;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.repository.LotRepository;
import com.krishilink.repository.OfferRepository;
import com.krishilink.repository.PaymentRepository;
import com.krishilink.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final LotRepository lotRepository;
    private final OfferRepository offerRepository;
    private final ObjectMapper objectMapper;

    private static final Map<TransactionStatus, EnumSet<TransactionStatus>> ALLOWED_TRANSITIONS =
            new EnumMap<>(Map.of(
                    TransactionStatus.CREATED, EnumSet.of(TransactionStatus.CANCELLED),
                    TransactionStatus.CONFIRMED, EnumSet.of(TransactionStatus.IN_TRANSIT,
                            TransactionStatus.CANCELLED, TransactionStatus.DISPUTED),
                    TransactionStatus.IN_TRANSIT, EnumSet.of(TransactionStatus.DELIVERED, TransactionStatus.DISPUTED),
                    TransactionStatus.DELIVERED, EnumSet.of(TransactionStatus.COMPLETED, TransactionStatus.DISPUTED)
            ));

    public static boolean isParticipant(Transaction tx, User user) {
        Long userId = user.getId();
        return (tx.getBuyer() != null && tx.getBuyer().getId().equals(userId))
                || (tx.getSeller() != null && tx.getSeller().getId().equals(userId));
    }

    public static void requireParticipantOrAdmin(Transaction tx, User user) {
        if (user.getRole() != Role.ADMIN && !isParticipant(tx, user)) {
            throw new AccessDeniedException("Not a participant in this transaction");
        }
    }

    public Transaction getTransactionForUser(Long id, User user) {
        Transaction tx = getTransactionById(id);
        requireParticipantOrAdmin(tx, user);
        return tx;
    }

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
        Transaction tx = transactionRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transaction not found with id: " + id));
        requireParticipantOrAdmin(tx, user);

        TransactionStatus status;
        try {
            status = TransactionStatus.valueOf(newStatus.toUpperCase().trim());
        } catch (Exception e) {
            throw new BadRequestException("Invalid transaction status");
        }

        boolean admin = user.getRole() == Role.ADMIN;
        if (!admin) {
            if (!ALLOWED_TRANSITIONS.getOrDefault(tx.getStatus(), EnumSet.noneOf(TransactionStatus.class))
                    .contains(status)) {
                throw new BadRequestException("Cannot move transaction from " + tx.getStatus() + " to " + status);
            }
            boolean buyer = tx.getBuyer() != null && tx.getBuyer().getId().equals(user.getId());
            if ((status == TransactionStatus.DELIVERED || status == TransactionStatus.COMPLETED) && !buyer) {
                throw new AccessDeniedException("Only the buyer can confirm delivery or release escrow");
            }
        }

        if (status == TransactionStatus.CANCELLED && tx.getStatus() != TransactionStatus.CANCELLED) {
            releaseReservedStock(tx);
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

    private void releaseReservedStock(Transaction tx) {
        Lot lot = tx.getLot();
        Double quantity = tx.getQuantity() != null ? tx.getQuantity()
                : tx.getOffer() != null ? tx.getOffer().getQuantity() : null;
        if (lot != null && quantity != null) {
            double remaining = (lot.getRemainingQuantity() != null ? lot.getRemainingQuantity() : 0) + quantity;
            lot.setRemainingQuantity(Math.min(remaining, lot.getQuantity()));
            if (lot.getStatus() == LotStatus.SOLD || lot.getStatus() == LotStatus.PARTIALLY_SOLD) {
                lot.setStatus(lot.getRemainingQuantity() >= lot.getQuantity()
                        ? LotStatus.AVAILABLE : LotStatus.PARTIALLY_SOLD);
            }
            lotRepository.save(lot);
        }
        Offer offer = tx.getOffer();
        if (offer != null) {
            offer.setStatus(OfferStatus.CANCELLED);
            offerRepository.save(offer);
        }
        paymentRepository.findByTransaction(tx).ifPresent(payment -> {
            if (payment.getStatus() == PaymentStatus.PAID) {
                payment.setStatus(PaymentStatus.REFUNDED);
                paymentRepository.save(payment);
            }
        });
    }
}
