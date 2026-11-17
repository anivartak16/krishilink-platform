package com.krishilink.service;

import com.krishilink.dto.LotDto;
import com.krishilink.entity.Lot;
import com.krishilink.entity.Quality;
import com.krishilink.entity.User;
import com.krishilink.enums.LotStatus;
import com.krishilink.enums.Role;
import com.krishilink.exception.BadRequestException;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.repository.LotRepository;
import com.krishilink.repository.QualityRepository;
import com.krishilink.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class LotService {

    private final LotRepository lotRepository;
    private final QualityRepository qualityRepository;

    @Transactional
    public Lot createLot(User owner, LotDto dto) {
        if (dto.getCommodity() == null || dto.getQuantity() == null || dto.getExpectedPrice() == null) {
            throw new BadRequestException("Commodity, quantity, and expected price are required");
        }

        Quality quality = buildQualityWithGradeDefaults(dto);

        String location = dto.getLocation() != null ? dto.getLocation() : owner.getLocation();
        List<Double> coords;
        if (dto.getLongitude() != null && dto.getLatitude() != null) {
            coords = List.of(dto.getLongitude(), dto.getLatitude());
        } else {
            coords = GeoUtils.resolveCoordinates(owner.getDistrict(), owner.getState(), location);
        }

        LotStatus status = LotStatus.AVAILABLE;
        if (owner.getRole() == Role.FARMER && !Boolean.TRUE.equals(owner.getKycVerified())) {
            status = LotStatus.PENDING_VERIFICATION;
        }

        Lot lot = Lot.builder()
                .owner(owner)
                .ownerType(dto.getOwnerType() != null ? dto.getOwnerType() : owner.getRole().name())
                .commodity(dto.getCommodity())
                .quantity(dto.getQuantity())
                .remainingQuantity(dto.getQuantity())
                .unit(dto.getUnit() != null ? dto.getUnit() : "KG")
                .harvestDate(dto.getHarvestDate())
                .location(location)
                .longitude(coords.get(0))
                .latitude(coords.get(1))
                .expectedPrice(dto.getExpectedPrice())
                .quality(quality)
                .availableUntil(dto.getAvailableUntil())
                .farmingType(dto.getFarmingType() != null ? dto.getFarmingType() : "conventional")
                .organicCertificationStatus(dto.getOrganicCertificationStatus() != null ? dto.getOrganicCertificationStatus() : "not_verified")
                .certificationType(dto.getCertificationType() != null ? dto.getCertificationType() : "")
                .certificateId(dto.getCertificateId())
                .certificateValidity(dto.getCertificateValidity())
                .certificateDocumentUrl(dto.getCertificateDocumentUrl())
                .status(status)
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
            // Default to available lots for public/marketplace view
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
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("owner").get("id"), ownerId));
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
                .orElseThrow(() -> new ResourceNotFoundException("Lot not found with id: " + id));
    }

    @Transactional
    public Lot updateLot(Long id, User user, LotDto dto) {
        Lot lot = getLotById(id);
        if (!lot.getOwner().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Unauthorized to update this lot");
        }

        if (dto.getCommodity() != null) lot.setCommodity(dto.getCommodity());
        if (dto.getQuantity() != null) lot.setQuantity(dto.getQuantity());
        if (dto.getExpectedPrice() != null) lot.setExpectedPrice(dto.getExpectedPrice());
        if (dto.getLocation() != null) lot.setLocation(dto.getLocation());
        if (dto.getAvailableUntil() != null) lot.setAvailableUntil(dto.getAvailableUntil());
        if (dto.getFarmingType() != null) lot.setFarmingType(dto.getFarmingType());

        // Update self-declared quality only if lot is not already verified by KVK
        if (dto.getGrade() != null && (lot.getQuality() == null || !"VERIFIED".equalsIgnoreCase(lot.getQuality().getInspectionStatus()))) {
            if (lot.getQuality() == null) {
                lot.setQuality(buildQualityWithGradeDefaults(dto));
            } else {
                updateQualityWithGradeDefaults(lot.getQuality(), dto);
            }
        }

        return lotRepository.save(lot);
    }

    @Transactional
    public void deleteLot(Long id, User user) {
        Lot lot = getLotById(id);
        if (!lot.getOwner().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Unauthorized to delete this lot");
        }
        lot.setStatus(LotStatus.CANCELLED);
        lotRepository.save(lot);
    }

    @Transactional
    public Lot publishLot(Long id, User user) {
        Lot lot = getLotById(id);
        if (!lot.getOwner().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only owner can publish this lot");
        }
        lot.setStatus(LotStatus.AVAILABLE);
        return lotRepository.save(lot);
    }

    @Transactional
    public Lot sendForVerification(Long id, User user, String notes) {
        Lot lot = getLotById(id);
        if (!lot.getOwner().getId().equals(user.getId()) && user.getRole() != Role.ADMIN) {
            throw new BadRequestException("Only owner can request verification");
        }
        lot.setStatus(LotStatus.PENDING_VERIFICATION);
        if (lot.getQuality() != null) {
            lot.getQuality().setInspectionStatus("PENDING");
            lot.getQuality().setInspectionNotes(notes != null ? notes : "Submitted for quality testing");
            qualityRepository.save(lot.getQuality());
        }
        return lotRepository.save(lot);
    }

    /**
     * Builds a Quality entity applying agricultural FAQ/grade-dependent standard baselines
     * when the farmer hasn't entered laboratory test values.
     */
    private Quality buildQualityWithGradeDefaults(LotDto dto) {
        if (dto.getGrade() == null && dto.getMoisture() == null &&
                dto.getForeignMatter() == null && dto.getDamagedPercentage() == null) {
            return null;
        }

        String rawGrade = dto.getGrade() != null ? dto.getGrade().trim() : "FAQ";
        GradeBaseline baseline = getGradeBaseline(rawGrade);

        Quality quality = Quality.builder()
                .grade(rawGrade)
                .moisture(dto.getMoisture() != null ? dto.getMoisture() : baseline.moisture)
                .foreignMatter(dto.getForeignMatter() != null ? dto.getForeignMatter() : baseline.foreignMatter)
                .damagedPercentage(dto.getDamagedPercentage() != null ? dto.getDamagedPercentage() : baseline.damagedPercentage)
                .certification(dto.getCertification())
                .inspectionStatus("PENDING")
                .build();

        return qualityRepository.save(quality);
    }

    private void updateQualityWithGradeDefaults(Quality quality, LotDto dto) {
        String rawGrade = dto.getGrade() != null ? dto.getGrade().trim() : quality.getGrade();
        GradeBaseline baseline = getGradeBaseline(rawGrade);

        quality.setGrade(rawGrade);
        quality.setMoisture(dto.getMoisture() != null ? dto.getMoisture() : baseline.moisture);
        quality.setForeignMatter(dto.getForeignMatter() != null ? dto.getForeignMatter() : baseline.foreignMatter);
        quality.setDamagedPercentage(dto.getDamagedPercentage() != null ? dto.getDamagedPercentage() : baseline.damagedPercentage);
        if (dto.getCertification() != null) quality.setCertification(dto.getCertification());
        qualityRepository.save(quality);
    }

    private record GradeBaseline(double moisture, double foreignMatter, double damagedPercentage) {}

    private GradeBaseline getGradeBaseline(String rawGrade) {
        String normalized = rawGrade != null ? rawGrade.toUpperCase().replace("-", "_").replace(" ", "_") : "FAQ";

        if (normalized.contains("GRADE_A") || normalized.equals("A") || normalized.contains("PREMIUM") || normalized.contains("SUPERIOR")) {
            // Grade A: Cleanest, low moisture, minimal defects
            return new GradeBaseline(11.0, 0.5, 0.5);
        } else if (normalized.contains("GRADE_B") || normalized.equals("B") || normalized.contains("MEDIUM")) {
            // Grade B: Good standard commercial grade
            return new GradeBaseline(12.0, 1.2, 2.0);
        } else if (normalized.contains("GRADE_C") || normalized.equals("C") || normalized.contains("LOW")) {
            // Grade C: Higher moisture tolerance, more foreign matter and damaged grain
            return new GradeBaseline(13.5, 2.5, 4.0);
        } else {
            // FAQ (Fair Average Quality) / Default Mandi Standard
            return new GradeBaseline(12.0, 1.0, 1.5);
        }
    }
}
