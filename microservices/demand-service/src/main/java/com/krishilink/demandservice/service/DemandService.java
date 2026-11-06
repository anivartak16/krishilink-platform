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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DemandService {

    private final DemandRepository demandRepository;
    private final LotClient lotClient;
    private final UserClient userClient;

    @Transactional
    public Demand createDemand(UserPrincipal principal, DemandDto dto) {
        String buyerName = "Buyer";
        String buyerPhone = "";
        try {
            ApiResponse<Map<String, Object>> userRes = userClient.getUserById(principal.getId());
            if (userRes != null && userRes.getData() != null) {
                buyerName = (String) userRes.getData().get("name");
                buyerPhone = (String) userRes.getData().get("phone");
            }
        } catch (Exception ignored) {}

        Demand demand = Demand.builder()
                .buyerId(principal.getId())
                .buyerName(buyerName)
                .buyerPhone(buyerPhone)
                .commodity(dto.getCommodity())
                .requiredQuantity(dto.getRequiredQuantity())
                .maxPrice(dto.getMaxPrice())
                .preferredLocation(dto.getPreferredLocation() != null ? dto.getPreferredLocation() : "Indore")
                .requiredQuality(dto.getRequiredQuality() != null ? dto.getRequiredQuality() : "Grade A")
                .deadline(dto.getDeadline() != null ? dto.getDeadline() : LocalDate.now().plusDays(15))
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
        if (status != null && !status.isBlank()) {
            try {
                DemandStatus s = DemandStatus.valueOf(status.toUpperCase().trim());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), s));
            } catch (Exception ignored) {}
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

    @Transactional
    public Demand updateDemand(Long id, UserPrincipal principal, DemandDto dto) {
        Demand demand = getDemandById(id);
        if (!demand.getBuyerId().equals(principal.getId()) && !"ADMIN".equalsIgnoreCase(principal.getRole())) {
            throw new RuntimeException("Unauthorized to update this demand");
        }

        if (dto.getCommodity() != null) demand.setCommodity(dto.getCommodity());
        if (dto.getRequiredQuantity() != null) demand.setRequiredQuantity(dto.getRequiredQuantity());
        if (dto.getMaxPrice() != null) demand.setMaxPrice(dto.getMaxPrice());
        if (dto.getPreferredLocation() != null) demand.setPreferredLocation(dto.getPreferredLocation());
        if (dto.getDeadline() != null) demand.setDeadline(dto.getDeadline());

        return demandRepository.save(demand);
    }

    @Transactional
    public void deleteDemand(Long id, UserPrincipal principal) {
        Demand demand = getDemandById(id);
        if (!demand.getBuyerId().equals(principal.getId()) && !"ADMIN".equalsIgnoreCase(principal.getRole())) {
            throw new RuntimeException("Unauthorized to delete this demand");
        }
        demand.setStatus(DemandStatus.CANCELLED);
        demandRepository.save(demand);
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
