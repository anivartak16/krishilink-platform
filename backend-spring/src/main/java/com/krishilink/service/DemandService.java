package com.krishilink.service;

import com.krishilink.dto.DemandDto;
import com.krishilink.entity.Demand;
import com.krishilink.entity.User;
import com.krishilink.enums.DemandStatus;
import com.krishilink.enums.Role;
import com.krishilink.exception.BadRequestException;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.repository.DemandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DemandService {

    private final DemandRepository demandRepository;

    @Transactional
    public Demand createDemand(User buyer, DemandDto dto) {
        if (dto.getCommodity() == null || dto.getRequiredQuantity() == null || dto.getMaxPrice() == null) {
            throw new BadRequestException("Commodity, required quantity, and max price are required");
        }

        Demand demand = Demand.builder()
                .buyer(buyer)
                .commodity(dto.getCommodity())
                .requiredQuantity(dto.getRequiredQuantity())
                .unit(dto.getUnit() != null ? dto.getUnit() : "KG")
                .requiredQuality(dto.getRequiredQuality() != null ? dto.getRequiredQuality() : "Grade A")
                .variety(dto.getVariety() != null ? dto.getVariety() : "All Varieties")
                .maxMoisture(dto.getMaxMoisture() != null ? dto.getMaxMoisture() : 12.0)
                .preferredLocation(dto.getPreferredLocation() != null ? dto.getPreferredLocation() : buyer.getLocation())
                .deliveryLocation(dto.getDeliveryLocation() != null ? dto.getDeliveryLocation() : buyer.getAddress())
                .maxPrice(dto.getMaxPrice())
                .paymentTerms(dto.getPaymentTerms() != null ? dto.getPaymentTerms() : "100% Escrow Secured")
                .contactPhone(dto.getContactPhone() != null ? dto.getContactPhone() : buyer.getPhone())
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

        if (status != null && !status.isBlank()) {
            try {
                DemandStatus demandStatus = DemandStatus.valueOf(status.toUpperCase().trim());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), demandStatus));
            } catch (Exception ignored) {}
        }

        if (buyerId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("buyer").get("id"), buyerId));
        }

        return demandRepository.findAll(spec);
    }

    public Demand getDemandById(Long id) {
        return demandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Demand not found with id: " + id));
    }

    @Transactional
    public Demand updateDemand(Long id, User user, DemandDto dto) {
        Demand demand = getDemandById(id);
        if (!demand.getBuyer().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Unauthorized to update this demand");
        }

        if (dto.getCommodity() != null) demand.setCommodity(dto.getCommodity());
        if (dto.getRequiredQuantity() != null) demand.setRequiredQuantity(dto.getRequiredQuantity());
        if (dto.getMaxPrice() != null) demand.setMaxPrice(dto.getMaxPrice());
        if (dto.getPreferredLocation() != null) demand.setPreferredLocation(dto.getPreferredLocation());
        if (dto.getDeadline() != null) demand.setDeadline(dto.getDeadline());

        return demandRepository.save(demand);
    }

    @Transactional
    public void deleteDemand(Long id, User user) {
        Demand demand = getDemandById(id);
        if (!demand.getBuyer().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Unauthorized to delete this demand");
        }
        demand.setStatus(DemandStatus.CANCELLED);
        demandRepository.save(demand);
    }
}
