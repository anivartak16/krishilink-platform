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
                .availableUntil(dto.getAvailableUntil())
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

        if (ownerId != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("ownerId"), ownerId));
        }

        return lotRepository.findAll(spec);
    }

    public Lot getLotById(Long id) {
        return lotRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Lot not found with id: " + id));
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
    public Lot resubmitLot(Long id, UserPrincipal principal) {
        Lot lot = getLotById(id);
        if (!lot.getOwnerId().equals(principal.getId()) && !"ADMIN".equalsIgnoreCase(principal.getRole())) {
            throw new RuntimeException("Unauthorized to resubmit this lot");
        }
        lot.setStatus(LotStatus.PENDING_VERIFICATION);
        return lotRepository.save(lot);
    }
}
