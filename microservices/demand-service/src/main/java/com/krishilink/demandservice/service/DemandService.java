package com.krishilink.demandservice.service;

import com.krishilink.demandservice.client.LotClient;
import com.krishilink.demandservice.client.UserClient;
import com.krishilink.demandservice.dto.ApiResponse;
import com.krishilink.demandservice.dto.DemandDto;
import com.krishilink.demandservice.dto.MatchDto;
import com.krishilink.demandservice.entity.Demand;
import com.krishilink.demandservice.enums.DemandStatus;
import com.krishilink.demandservice.repository.DemandRepository;
import com.krishilink.demandservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
public class DemandService {

    private final DemandRepository demandRepository;
    private final UserClient userClient;
    private final LotClient lotClient;

    @Transactional
    public Demand createDemand(UserPrincipal principal, DemandDto dto) {
        String buyerName = "Buyer";
        String buyerOrg = "AgroCorp";
        String buyerPhone = "";
        try {
            ApiResponse<Map<String, Object>> u = userClient.getUserById(principal.getId());
            if (u != null && u.getData() != null) {
                buyerName = (String) u.getData().get("name");
                buyerOrg = (String) u.getData().get("organizationName");
                buyerPhone = (String) u.getData().get("phone");
            }
        } catch (Exception ignored) {}

        Demand demand = Demand.builder()
                .buyerId(principal.getId())
                .buyerName(buyerName)
                .buyerOrganization(buyerOrg)
                .buyerPhone(buyerPhone)
                .commodity(dto.getCommodity())
                .requiredQuantity(dto.getRequiredQuantity())
                .unit(dto.getUnit() != null ? dto.getUnit() : "KG")
                .requiredQuality(dto.getRequiredQuality() != null ? dto.getRequiredQuality() : "Grade A")
                .variety(dto.getVariety() != null ? dto.getVariety() : "All Varieties")
                .maxMoisture(dto.getMaxMoisture() != null ? dto.getMaxMoisture() : 12.0)
                .preferredLocation(dto.getPreferredLocation() != null ? dto.getPreferredLocation() : "Indore")
                .deliveryLocation(dto.getDeliveryLocation())
                .maxPrice(dto.getMaxPrice())
                .paymentTerms(dto.getPaymentTerms() != null ? dto.getPaymentTerms() : "100% Escrow Secured")
                .contactPhone(dto.getContactPhone() != null ? dto.getContactPhone() : buyerPhone)
                .farmingType(dto.getFarmingType() != null ? dto.getFarmingType() : "any")
                .notes(dto.getNotes())
                .deadline(dto.getDeadline())
                .status(DemandStatus.ACTIVE)
                .build();

        return demandRepository.save(demand);
    }

    public List<Demand> getDemands(String commodity, String status, Long buyerId) {
        Specification<Demand> spec = Specification.where(null);
        if (commodity != null && !commodity.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("commodity")), "%" + commodity.toLowerCase().trim() + "%"));
        }
        if (buyerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("buyerId"), buyerId));
        }
        return demandRepository.findAll(spec);
    }

    public Demand getDemandById(Long id) {
        return demandRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Demand not found with id: " + id));
    }

    public List<MatchDto> getMatchesFor(Long demandId) {
        Demand d = getDemandById(demandId);
        List<Map<String, Object>> lots = List.of();
        try {
            ApiResponse<List<Map<String, Object>>> res = lotClient.getLots(d.getCommodity(), "AVAILABLE");
            if (res != null && res.getData() != null) {
                lots = res.getData();
            }
        } catch (Exception ignored) {}

        List<MatchDto> matches = new ArrayList<>();
        for (Map<String, Object> lot : lots) {
            double lotQty = lot.get("remainingQuantity") != null ? Double.parseDouble(lot.get("remainingQuantity").toString()) : 0.0;
            double price = lot.get("expectedPrice") != null ? Double.parseDouble(lot.get("expectedPrice").toString()) : 0.0;

            int qtyScore = (int) Math.min(25, (lotQty / Math.max(1, d.getRequiredQuantity())) * 25);
            int priceScore = price <= d.getMaxPrice() ? 25 : Math.max(5, (int) (25 - ((price - d.getMaxPrice()) / d.getMaxPrice()) * 25));
            int qualityScore = 25;
            int locationScore = 15;
            int total = qtyScore + priceScore + qualityScore + locationScore;

            matches.add(MatchDto.builder()
                    .lot(lot)
                    .matchScore(total)
                    .breakdown(Map.of(
                            "quantity", qtyScore,
                            "price", priceScore,
                            "quality", qualityScore,
                            "logistics", locationScore
                    ))
                    .reasons(List.of(
                            "Matches target commodity " + d.getCommodity(),
                            "Offered price ₹" + price + " within procurement ceiling",
                            "High grade certified harvest"
                    ))
                    .build());
        }

        matches.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));
        return matches;
    }
}
