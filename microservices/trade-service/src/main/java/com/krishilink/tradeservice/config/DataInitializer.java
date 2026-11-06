package com.krishilink.tradeservice.config;

import com.krishilink.tradeservice.entity.Offer;
import com.krishilink.tradeservice.entity.Payment;
import com.krishilink.tradeservice.entity.Transaction;
import com.krishilink.tradeservice.enums.OfferStatus;
import com.krishilink.tradeservice.enums.PaymentStatus;
import com.krishilink.tradeservice.enums.TransactionStatus;
import com.krishilink.tradeservice.repository.OfferRepository;
import com.krishilink.tradeservice.repository.PaymentRepository;
import com.krishilink.tradeservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final OfferRepository offerRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;

    @Override
    public void run(String... args) {
        if (offerRepository.count() > 0) return;

        log.info("Initializing Trade Service demo trades, offers, and escrow payments...");

        Offer offer = Offer.builder()
                .lotId(1L)
                .commodity("Wheat")
                .buyerId(3L)
                .buyerName("AgroCorp Direct")
                .buyerPhone("9876543213")
                .sellerId(2L)
                .sellerName("Ramesh Patel")
                .quantity(1000.0)
                .pricePerUnit(2450.0)
                .totalAmount(2450000.0)
                .message("Immediate pickup from Indore warehouse with full escrow guarantee.")
                .status(OfferStatus.ACCEPTED)
                .build();
        offer = offerRepository.save(offer);

        Transaction tx = Transaction.builder()
                .offerId(offer.getId())
                .lotId(1L)
                .commodity("Wheat")
                .buyerId(3L)
                .buyerName("AgroCorp Direct")
                .sellerId(2L)
                .sellerName("Ramesh Patel")
                .quantity(1000.0)
                .amount(2450000.0)
                .status(TransactionStatus.CONFIRMED)
                .build();
        tx = transactionRepository.save(tx);

        Payment payment = Payment.builder()
                .transactionId(tx.getId())
                .amount(2450000.0)
                .paymentMethod("UPI / Escrow")
                .referenceId("KL-ESCROW-2024-99881")
                .remarks("KrishiLink Escrow Balance Locked")
                .status(PaymentStatus.PAID)
                .paidAt(LocalDateTime.now())
                .build();
        paymentRepository.save(payment);

        log.info("Trade Service initialized with demo contracts!");
    }
}
