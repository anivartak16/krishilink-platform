package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.Lot;
import com.krishilink.entity.Notification;
import com.krishilink.entity.Quality;
import com.krishilink.enums.LotStatus;
import com.krishilink.exception.BadRequestException;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.repository.LotRepository;
import com.krishilink.repository.NotificationRepository;
import com.krishilink.repository.QualityRepository;
import com.krishilink.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/inspections")
@RequiredArgsConstructor
public class InspectionController {

    private final LotRepository lotRepository;
    private final QualityRepository qualityRepository;
    private final NotificationRepository notificationRepository;

    @GetMapping("/lots")
    public ResponseEntity<ApiResponse<List<Lot>>> getInspectionLots(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String commodity) {
        List<Lot> lots;
        if (status != null && !status.isBlank()) {
            LotStatus s = LotStatus.valueOf(status.toUpperCase());
            lots = lotRepository.findAll().stream()
                    .filter(l -> l.getStatus() == s)
                    .filter(l -> commodity == null || commodity.isBlank() || l.getCommodity().equalsIgnoreCase(commodity))
                    .toList();
        } else {
            lots = lotRepository.findAll().stream()
                    .filter(l -> l.getStatus() == LotStatus.PENDING_VERIFICATION ||
                            l.getStatus() == LotStatus.VERIFIED ||
                            l.getStatus() == LotStatus.REJECTED ||
                            l.getStatus() == LotStatus.AVAILABLE ||
                            l.getStatus() == LotStatus.PARTIALLY_SOLD)
                    .filter(l -> commodity == null || commodity.isBlank() || l.getCommodity().equalsIgnoreCase(commodity))
                    .toList();
        }
        return ResponseEntity.ok(ApiResponse.ok(lots));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<Lot>> createInspection(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Long lotId = Long.valueOf(body.get("lotId").toString());
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Lot not found"));

        String inspectionStatus = body.get("inspectionStatus") != null ?
                body.get("inspectionStatus").toString() : "VERIFIED";
        String grade = body.get("grade") != null ? body.get("grade").toString() : "Grade A";
        Double moisture = body.get("moisture") != null && !body.get("moisture").toString().isBlank() ?
                Double.valueOf(body.get("moisture").toString()) : 12.0;
        Double foreignMatter = body.get("foreignMatter") != null && !body.get("foreignMatter").toString().isBlank() ?
                Double.valueOf(body.get("foreignMatter").toString()) : 1.0;
        Double damaged = body.get("damagedPercentage") != null && !body.get("damagedPercentage").toString().isBlank() ?
                Double.valueOf(body.get("damagedPercentage").toString()) : 0.5;
        String defects = body.get("defects") != null ? body.get("defects").toString() : "";
        String grainImage = body.get("grainImage") != null ? body.get("grainImage").toString() : null;
        String notes = body.get("inspectionNotes") != null ? body.get("inspectionNotes").toString() : "";

        Quality quality = lot.getQuality();
        if (quality == null) {
            quality = new Quality();
        }

        String centerName = principal != null && principal.getUser().getOrganizationName() != null ?
                principal.getUser().getOrganizationName() : "Krishi Vigyan Kendra";

        quality.setGrade("REJECTED".equalsIgnoreCase(inspectionStatus) ? "Rejected" : grade);
        quality.setMoisture(moisture);
        quality.setForeignMatter(foreignMatter);
        quality.setDamagedPercentage(damaged);
        quality.setDefects(defects);
        quality.setGrainImage(grainImage);
        quality.setInspectionNotes(notes);
        quality.setInspectionStatus(inspectionStatus);
        quality.setInspectedByName(centerName);
        quality.setInspectedAt(LocalDateTime.now());
        quality.setCertification("VERIFIED".equalsIgnoreCase(inspectionStatus) ?
                "Krishi Vigyan Kendra Certified (" + centerName + ")" :
                "Inspection Rejected (" + centerName + ")");
        quality = qualityRepository.save(quality);

        lot.setQuality(quality);
        if ("VERIFIED".equalsIgnoreCase(inspectionStatus)) {
            lot.setStatus(LotStatus.VERIFIED);
        } else if ("REJECTED".equalsIgnoreCase(inspectionStatus)) {
            lot.setStatus(LotStatus.REJECTED);
        }
        lot = lotRepository.save(lot);

        if (lot.getOwner() != null) {
            Notification notif = Notification.builder()
                    .user(lot.getOwner())
                    .type("VERIFIED".equalsIgnoreCase(inspectionStatus) ? "LOT_VERIFIED" : "LOT_INSPECTION_REJECTED")
                    .message("VERIFIED".equalsIgnoreCase(inspectionStatus) ?
                            "✓ Your lot of " + lot.getCommodity() + " (" + lot.getRemainingQuantity() + " kg) was verified by " + centerName + " as " + grade + ". You can now list it on the marketplace!" :
                            "✕ Inspection Notice: Your lot of " + lot.getCommodity() + " was rejected by " + centerName + ". Reason: " + (defects.isBlank() ? notes : defects))
                    .build();
            notificationRepository.save(notif);
        }

        return ResponseEntity.ok(ApiResponse.ok(lot, "VERIFIED".equalsIgnoreCase(inspectionStatus) ?
                "Quality verified and certified! Farmer can now list produce on the marketplace." :
                "Inspection recorded as rejected."));
    }
}
