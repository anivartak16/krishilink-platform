package com.krishilink.lotservice.service;

import com.krishilink.lotservice.client.UserClient;
import com.krishilink.lotservice.dto.ApiResponse;
import com.krishilink.lotservice.dto.LotDto;
import com.krishilink.lotservice.entity.Lot;
import com.krishilink.lotservice.entity.Quality;
import com.krishilink.lotservice.enums.LotStatus;
import com.krishilink.lotservice.repository.LotRepository;
import com.krishilink.lotservice.repository.QualityRepository;
import com.krishilink.lotservice.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class LotService {

    private final LotRepository lotRepository;
    private final QualityRepository qualityRepository;
    private final UserClient userClient;

    @Transactional
    public Lot createLot(UserPrincipal principal, LotDto dto) {
        String ownerName = "Farmer";
        String ownerPhone = "";
        try {
            ApiResponse<Map<String, Object>> userRes = userClient.getUserById(principal.getId());
            if (userRes != null && userRes.getData() != null) {
                ownerName = (String) userRes.getData().get("name");
                ownerPhone = (String) userRes.getData().get("phone");
            }
        } catch (Exception ignored) {}

        Quality quality = null;
        if (dto.getGrade() != null) {
            quality = Quality.builder()
                    .grade(dto.getGrade())
                    .moisture(dto.getMoisture() != null ? dto.getMoisture() : 11.5)
                    .foreignMatter(dto.getForeignMatter() != null ? dto.getForeignMatter() : 0.8)
                    .damagedPercentage(dto.getDamagedPercentage() != null ? dto.getDamagedPercentage() : 1.0)
                    .certification(dto.getCertification())
                    .build();
            quality = qualityRepository.save(quality);
        }

        Lot lot = Lot.builder()
                .ownerId(principal.getId())
                .ownerName(ownerName)
                .ownerPhone(ownerPhone)
                .ownerType(dto.getOwnerType() != null ? dto.getOwnerType() : principal.getRole())
                .commodity(dto.getCommodity())
                .quantity(dto.getQuantity())
                .remainingQuantity(dto.getQuantity())
                .unit(dto.getUnit() != null ? dto.getUnit() : "KG")
                .harvestDate(dto.getHarvestDate())
                .location(dto.getLocation() != null ? dto.getLocation() : "Indore")
                .longitude(dto.getLongitude() != null ? dto.getLongitude() : 75.8577)
                .latitude(dto.getLatitude() != null ? dto.getLatitude() : 22.7196)
                .expectedPrice(dto.getExpectedPrice())
                .quality(quality)
                .availableUntil(dto.getAvailableUntil() != null ? dto.getAvailableUntil() : LocalDate.now().plusDays(30))
                .farmingType(dto.getFarmingType() != null ? dto.getFarmingType() : "conventional")
                .organicCertificationStatus(dto.getOrganicCertificationStatus() != null ? dto.getOrganicCertificationStatus() : "not_verified")
                .certificationType(dto.getCertificationType())
                .certificateId(dto.getCertificateId())
                .certificateValidity(dto.getCertificateValidity())
                .certificateDocumentUrl(dto.getCertificateDocumentUrl())
                .status(LotStatus.PENDING_VERIFICATION)
                .build();

        return lotRepository.save(lot);
    }

    public List<Lot> getLots(String commodity, String status, String farmingType,
                             Double minPrice, Double maxPrice, Long ownerId, String search) {
        Specification<Lot> spec = Specification.where(null);

        if (commodity != null && !commodity.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("commodity")), "%" + commodity.toLowerCase().trim() + "%"));
        }

        if (status != null && !status.isBlank()) {
            try {
                LotStatus lotStatus = LotStatus.valueOf(status.toUpperCase().trim());
                spec = spec.and((root, query, cb) -> cb.equal(root.get("status"), lotStatus));
            } catch (Exception ignored) {}
        } else if (ownerId == null) {
            spec = spec.and((root, query, cb) ->
                    root.get("status").in(LotStatus.AVAILABLE, LotStatus.PARTIALLY_SOLD));
        }

        if (farmingType != null && !farmingType.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(cb.lower(root.get("farmingType")), farmingType.toLowerCase().trim()));
        }

        if (minPrice != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("expectedPrice"), minPrice));
        }

        if (maxPrice != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("expectedPrice"), maxPrice));
        }

        if (ownerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ownerId"), ownerId));
        }

        if (search != null && !search.isBlank()) {
            String s = "%" + search.toLowerCase().trim() + "%";
            spec = spec.and((root, query, cb) ->
                    cb.or(
                            cb.like(cb.lower(root.get("commodity")), s),
                            cb.like(cb.lower(root.get("location")), s)
                    ));
        }

        return lotRepository.findAll(spec);
    }

    public Lot getLotById(Long id) {
        return lotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lot not found with id: " + id));
    }

    @Transactional
    public Lot updateLot(Long id, UserPrincipal principal, LotDto dto) {
        Lot lot = getLotById(id);
        if (!lot.getOwnerId().equals(principal.getId()) && !"ADMIN".equalsIgnoreCase(principal.getRole())) {
            throw new RuntimeException("Unauthorized to update this lot");
        }

        if (dto.getCommodity() != null) lot.setCommodity(dto.getCommodity());
        if (dto.getQuantity() != null) lot.setQuantity(dto.getQuantity());
        if (dto.getExpectedPrice() != null) lot.setExpectedPrice(dto.getExpectedPrice());
        if (dto.getLocation() != null) lot.setLocation(dto.getLocation());
        if (dto.getAvailableUntil() != null) lot.setAvailableUntil(dto.getAvailableUntil());
        if (dto.getFarmingType() != null) lot.setFarmingType(dto.getFarmingType());

        return lotRepository.save(lot);
    }

    @Transactional
    public void deleteLot(Long id, UserPrincipal principal) {
        Lot lot = getLotById(id);
        if (!lot.getOwnerId().equals(principal.getId()) && !"ADMIN".equalsIgnoreCase(principal.getRole())) {
            throw new RuntimeException("Unauthorized to delete this lot");
        }
        lot.setStatus(LotStatus.CANCELLED);
        lotRepository.save(lot);
    }

    @Transactional
    public Lot publishLot(Long id, UserPrincipal principal) {
        Lot lot = getLotById(id);
        if (!lot.getOwnerId().equals(principal.getId()) && !"ADMIN".equalsIgnoreCase(principal.getRole())) {
            throw new RuntimeException("Unauthorized to publish this lot");
        }
        lot.setStatus(LotStatus.AVAILABLE);
        return lotRepository.save(lot);
    }

    @Transactional
    public Lot sendForVerification(Long id, UserPrincipal principal, String notes) {
        Lot lot = getLotById(id);
        if (!lot.getOwnerId().equals(principal.getId()) && !"ADMIN".equalsIgnoreCase(principal.getRole())) {
            throw new RuntimeException("Unauthorized to request verification");
        }
        lot.setStatus(LotStatus.PENDING_VERIFICATION);
        if (lot.getQuality() != null) {
            lot.getQuality().setInspectionStatus("PENDING");
            lot.getQuality().setInspectionNotes(notes != null ? notes : "Submitted for quality testing");
            qualityRepository.save(lot.getQuality());
        }
        return lotRepository.save(lot);
    }

    @Transactional
    public Lot resubmitLot(Long id, UserPrincipal principal) {
        Lot lot = getLotById(id);
        if (!lot.getOwnerId().equals(principal.getId()) && !"ADMIN".equalsIgnoreCase(principal.getRole())) {
            throw new RuntimeException("Unauthorized to resubmit this lot");
        }
        lot.setStatus(LotStatus.PENDING_VERIFICATION);
        return lotRepository.save(lot);
    }

    public List<Lot> getLotsByOwnerIds(Collection<Long> ownerIds) {
        if (ownerIds == null || ownerIds.isEmpty()) return List.of();
        return lotRepository.findByOwnerIdInAndStatusIn(
                ownerIds,
                List.of(LotStatus.AVAILABLE, LotStatus.PARTIALLY_SOLD)
        );
    }

    @Transactional
    public Lot aggregateLots(Long fpoId, String fpoName, String location, Double lat, Double lon,
                             List<Long> lotIds, Double customPrice) {
        List<Lot> source = lotRepository.findAllById(lotIds);
        if (source.isEmpty()) {
            throw new RuntimeException("No valid lots found for aggregation");
        }

        String commodity = source.get(0).getCommodity();
        double totalQty = source.stream().mapToDouble(l -> l.getRemainingQuantity() != null ? l.getRemainingQuantity() : l.getQuantity()).sum();
        double expectedPrice = customPrice != null && customPrice > 0 ? customPrice :
                source.stream().mapToDouble(Lot::getExpectedPrice).average().orElse(25.0);

        Lot aggregated = Lot.builder()
                .ownerId(fpoId)
                .ownerName(fpoName != null ? fpoName : "FPO Aggregation")
                .ownerType("FPO")
                .commodity(commodity)
                .quantity(totalQty)
                .remainingQuantity(totalQty)
                .expectedPrice(expectedPrice)
                .unit(source.get(0).getUnit())
                .location(location != null ? location : "Central Hub")
                .latitude(lat != null ? lat : 22.7196)
                .longitude(lon != null ? lon : 75.8577)
                .quality(source.get(0).getQuality())
                .status(LotStatus.AVAILABLE)
                .availableUntil(LocalDate.now().plusDays(14))
                .farmingType(source.get(0).getFarmingType())
                .build();

        aggregated = lotRepository.save(aggregated);

        for (Lot s : source) {
            s.setStatus(LotStatus.CANCELLED);
            lotRepository.save(s);
        }

        return aggregated;
    }
}
