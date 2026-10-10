package com.krishilink.service;

import com.krishilink.entity.Demand;
import com.krishilink.entity.Lot;
import com.krishilink.entity.User;
import com.krishilink.enums.*;
import com.krishilink.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final LotRepository lotRepository;
    private final DemandRepository demandRepository;
    private final OfferRepository offerRepository;
    private final TransactionRepository transactionRepository;
    private final UserRepository userRepository;

    public Map<String, Object> getSummary(User user) {
        Map<String, Object> res = new LinkedHashMap<>();

        if (user.getRole() == Role.BUYER) {
            long activeDemands = demandRepository.findByBuyer(user).stream()
                    .filter(d -> d.getStatus() == DemandStatus.ACTIVE).count();
            long pendingOffers = offerRepository.findByBuyer(user).stream()
                    .filter(o -> o.getStatus() == OfferStatus.PENDING).count();
            long activeTransactions = transactionRepository.findByBuyer(user).stream()
                    .filter(t -> t.getStatus() != TransactionStatus.COMPLETED && t.getStatus() != TransactionStatus.CANCELLED).count();
            List<Lot> availableLots = lotRepository.findByStatusIn(List.of(LotStatus.AVAILABLE, LotStatus.PARTIALLY_SOLD));
            double totalVolume = availableLots.stream()
                    .mapToDouble(l -> l.getRemainingQuantity() != null ? l.getRemainingQuantity() : 0.0).sum();
            List<Demand> recentDemands = demandRepository.findByBuyer(user).stream().limit(4).toList();

            res.put("role", "BUYER");
            res.put("activeDemands", activeDemands);
            res.put("pendingOffers", pendingOffers);
            res.put("activeTransactions", activeTransactions);
            res.put("availableLots", availableLots.size());
            res.put("availableVolume", totalVolume);
            res.put("recentDemands", recentDemands);
            return res;
        }

        if (user.getRole() == Role.FPO) {
            List<Lot> fpoLots = lotRepository.findByOwner(user);
            long activeLots = fpoLots.stream()
                    .filter(l -> l.getStatus() == LotStatus.AVAILABLE || l.getStatus() == LotStatus.PARTIALLY_SOLD).count();
            double pooledVolume = fpoLots.stream()
                    .filter(l -> l.getStatus() == LotStatus.AVAILABLE || l.getStatus() == LotStatus.PARTIALLY_SOLD)
                    .mapToDouble(l -> l.getRemainingQuantity() != null ? l.getRemainingQuantity() : 0.0).sum();
            long pendingOffers = offerRepository.findByLotOwner(user).stream()
                    .filter(o -> o.getStatus() == OfferStatus.PENDING).count();

            res.put("role", "FPO");
            res.put("members", user.getMemberCount() != null ? user.getMemberCount() : 120);
            res.put("activeLots", activeLots);
            res.put("pooledVolume", pooledVolume);
            res.put("pendingOffers", pendingOffers);
            return res;
        }

        // FARMER / default
        List<Lot> farmerLots = lotRepository.findByOwner(user);
        long activeLots = farmerLots.stream()
                .filter(l -> l.getStatus() == LotStatus.AVAILABLE || l.getStatus() == LotStatus.PARTIALLY_SOLD).count();
        double volume = farmerLots.stream()
                .filter(l -> l.getStatus() == LotStatus.AVAILABLE || l.getStatus() == LotStatus.PARTIALLY_SOLD)
                .mapToDouble(l -> l.getRemainingQuantity() != null ? l.getRemainingQuantity() : 0.0).sum();
        long pendingOffers = offerRepository.findByLotOwner(user).stream()
                .filter(o -> o.getStatus() == OfferStatus.PENDING).count();
        long activeTransactions = transactionRepository.findBySeller(user).stream()
                .filter(t -> t.getStatus() != TransactionStatus.COMPLETED && t.getStatus() != TransactionStatus.CANCELLED).count();
        long pendingVerificationLots = farmerLots.stream()
                .filter(l -> l.getStatus() == LotStatus.PENDING_VERIFICATION).count();
        long verifiedLotsReadyToPublish = farmerLots.stream()
                .filter(l -> l.getStatus() == LotStatus.VERIFIED).count();

        res.put("role", "FARMER");
        res.put("activeLots", activeLots);
        res.put("availableVolume", volume);
        res.put("pendingOffers", pendingOffers);
        res.put("activeTransactions", activeTransactions);
        res.put("pendingVerificationLots", pendingVerificationLots);
        res.put("verifiedLotsReadyToPublish", verifiedLotsReadyToPublish);
        return res;
    }
}
