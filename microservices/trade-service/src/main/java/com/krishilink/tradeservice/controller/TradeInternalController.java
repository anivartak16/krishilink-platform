package com.krishilink.tradeservice.controller;

import com.krishilink.tradeservice.dto.ApiResponse;
import com.krishilink.tradeservice.entity.Offer;
import com.krishilink.tradeservice.entity.Payment;
import com.krishilink.tradeservice.entity.Transaction;
import com.krishilink.tradeservice.repository.OfferRepository;
import com.krishilink.tradeservice.repository.PaymentRepository;
import com.krishilink.tradeservice.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/trade-internal")
@RequiredArgsConstructor
public class TradeInternalController {

    private final OfferRepository offerRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;

    @GetMapping("/offers")
    public ResponseEntity<ApiResponse<List<Offer>>> getAllOffers() {
        return ResponseEntity.ok(ApiResponse.ok(offerRepository.findAll()));
    }

    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<Transaction>>> getAllTransactions() {
        return ResponseEntity.ok(ApiResponse.ok(transactionRepository.findAll()));
    }

    @GetMapping("/payments")
    public ResponseEntity<ApiResponse<List<Payment>>> getAllPayments() {
        return ResponseEntity.ok(ApiResponse.ok(paymentRepository.findAll()));
    }
}
