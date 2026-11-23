package com.krishilink.service;

import com.krishilink.outbox.OutboxPublisher;
import com.krishilink.outbox.TradeEvents;
import com.krishilink.util.Money;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishilink.dto.OfferDto;
import com.krishilink.entity.*;
import com.krishilink.enums.*;
import com.krishilink.exception.BadRequestException;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.repository.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.LockModeType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OfferService {

    private static final java.util.Set<LotStatus> OFFERABLE_LOT_STATUSES = java.util.EnumSet.of(
            LotStatus.AVAILABLE, LotStatus.PARTIALLY_SOLD, LotStatus.VERIFIED);

    private final OfferRepository offerRepository;
    private final LotRepository lotRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final OutboxPublisher outboxPublisher;
    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    @Transactional
    public Offer createOffer(User buyer, OfferDto dto) {
        Lot lot = lotRepository.findById(dto.getLotId())
                .orElseThrow(() -> new ResourceNotFoundException("Lot not found"));

        if (lot.getOwner().getId().equals(buyer.getId())) {
            throw new BadRequestException("You cannot make an offer on your own lot");
        }
        if (!OFFERABLE_LOT_STATUSES.contains(lot.getStatus())) {
            throw new BadRequestException("This lot is not open for offers");
        }
        if (dto.getQuantity() == null || dto.getQuantity() <= 0
                || dto.getPricePerUnit() == null || dto.getPricePerUnit().signum() <= 0) {
            throw new BadRequestException("Quantity and price per unit must be greater than zero");
        }
        if (lot.getRemainingQuantity() < dto.getQuantity()) {
            throw new BadRequestException("Requested quantity exceeds available stock (" + lot.getRemainingQuantity() + " kg)");
        }

        BigDecimal pricePerUnit = Money.of(dto.getPricePerUnit());
        BigDecimal totalAmount = Money.total(pricePerUnit, dto.getQuantity());

        Offer offer = Offer.builder()
                .lot(lot)
                .buyer(buyer)
                .quantity(dto.getQuantity())
                .pricePerUnit(pricePerUnit)
                .totalAmount(totalAmount)
                .message(dto.getMessage())
                .validUntil(dto.getValidUntil())
                .status(OfferStatus.PENDING)
                .build();

        offer = offerRepository.save(offer);

        outboxPublisher.publish("Offer", offer.getId(), TradeEvents.OFFER_PLACED, Map.of(
                "sellerId", lot.getOwner().getId(),
                "buyerName", buyer.getName(),
                "quantity", offer.getQuantity(),
                "commodity", lot.getCommodity()));

        return offer;
    }

    public List<Offer> getOffers(User user, String status) {
        if (user.getRole() == Role.BUYER) {
            return offerRepository.findByBuyer(user);
        } else {
            return offerRepository.findByLotOwner(user);
        }
    }

    public Offer getOfferById(Long id) {
        return offerRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Offer not found with id: " + id));
    }

    public Offer getOfferForUser(Long id, User user) {
        Offer offer = getOfferById(id);
        boolean buyer = offer.getBuyer() != null && offer.getBuyer().getId().equals(user.getId());
        boolean seller = offer.getLot().getOwner().getId().equals(user.getId());
        if (!buyer && !seller && user.getRole() != Role.ADMIN) {
            throw new AccessDeniedException("Not a party to this offer");
        }
        return offer;
    }

    @Transactional
    public Transaction acceptOffer(Long id, User user) {
        Offer offer = getOfferById(id);
        Lot lot = offer.getLot();
        // Serialize acceptances per lot so stock checks see committed quantities.
        entityManager.refresh(lot, LockModeType.PESSIMISTIC_WRITE);
        entityManager.refresh(offer);

        if (!lot.getOwner().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only the seller can accept this offer");
        }

        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new BadRequestException("Offer is already " + offer.getStatus());
        }

        if (lot.getRemainingQuantity() < offer.getQuantity()) {
            throw new BadRequestException("Not enough remaining quantity on lot to accept this offer");
        }

        // 1. Accept offer
        offer.setStatus(OfferStatus.ACCEPTED);
        offerRepository.save(offer);

        // 2. Decrement lot remaining quantity
        double newRemaining = lot.getRemainingQuantity() - offer.getQuantity();
        lot.setRemainingQuantity(newRemaining);
        if (newRemaining <= 0) {
            lot.setStatus(LotStatus.SOLD);
        } else {
            lot.setStatus(LotStatus.PARTIALLY_SOLD);
        }
        lotRepository.save(lot);

        // 3. Create Transaction
        String eventsJson = "";
        try {
            eventsJson = objectMapper.writeValueAsString(List.of(
                    Map.of("status", "CREATED", "note", "Offer accepted and trade created", "at", LocalDateTime.now().toString())
            ));
        } catch (Exception ignored) {}

        Transaction tx = Transaction.builder()
                .offer(offer)
                .lot(lot)
                .buyer(offer.getBuyer())
                .seller(lot.getOwner())
                .quantity(offer.getQuantity())
                .amount(offer.getTotalAmount())
                .status(TransactionStatus.CREATED)
                .eventsJson(eventsJson)
                .build();

        tx = transactionRepository.save(tx);

        // 4. Create Payment record
        Payment payment = Payment.builder()
                .transaction(tx)
                .amount(offer.getTotalAmount())
                .paymentMethod("Bank Transfer")
                .status(PaymentStatus.PROCESSING)
                .remarks("Escrow release after delivery")
                .build();
        paymentRepository.save(payment);

        // 5. Event for notifications, committed with the trade
        outboxPublisher.publish("Transaction", tx.getId(), TradeEvents.OFFER_ACCEPTED, Map.of(
                "offerId", offer.getId(),
                "buyerId", offer.getBuyer().getId(),
                "sellerId", lot.getOwner().getId(),
                "quantity", offer.getQuantity(),
                "commodity", lot.getCommodity()));

        return tx;
    }

    @Transactional
    public Offer rejectOffer(Long id, User user) {
        Offer offer = getOfferById(id);
        Lot lot = offer.getLot();

        if (!lot.getOwner().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only the seller can reject this offer");
        }
        if (offer.getStatus() != OfferStatus.PENDING) {
            throw new BadRequestException("Only pending offers can be rejected");
        }

        offer.setStatus(OfferStatus.REJECTED);
        return offerRepository.save(offer);
    }
}
