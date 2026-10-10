package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.Lot;
import com.krishilink.entity.Quality;
import com.krishilink.entity.User;
import com.krishilink.enums.LotStatus;
import com.krishilink.enums.Role;
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
            @AuthenticationPrincipal UserPrincipal principal) {
        requireInspector(principal.getUser());
        return ResponseEntity.ok(ApiResponse.ok(lotRepository.findAll()));
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ApiResponse<Lot>> inspect(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        User inspector = requireInspector(principal.getUser());
        Long lotId = longValue(body.get("lotId"));
        Lot lot = lotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Lot not found"));
        String decision = text(body.get("inspectionStatus"), "inspectionStatus").toUpperCase();
        if (!decision.equals("VERIFIED") && !decision.equals("REJECTED") && !decision.equals("PENDING")) {
            throw new BadRequestException("inspectionStatus must be VERIFIED, REJECTED, or PENDING");
        }

        Quality quality = lot.getQuality();
        if (quality == null) {
            quality = Quality.builder().grade(textOr(body.get("grade"), "Grade A")).build();
            lot.setQuality(quality);
        }
        quality.setGrade(textOr(body.get("grade"), quality.getGrade()));
        quality.setMoisture(number(body.get("moisture"), quality.getMoisture()));
        quality.setForeignMatter(number(body.get("foreignMatter"), quality.getForeignMatter()));
        quality.setDamagedPercentage(number(body.get("damagedPercentage"), quality.getDamagedPercentage()));
        quality.setDefects(textOrNull(body.get("defects")));
        quality.setInspectionNotes(textOrNull(body.get("inspectionNotes")));
        quality.setGrainImage(textOrNull(body.get("grainImage")));
        quality.setInspectionStatus(decision);
        quality.setInspectedAt(LocalDateTime.now());
        quality.setInspectedByName(inspector.getName());
        qualityRepository.save(quality);

        lot.setStatus(decision.equals("VERIFIED") ? LotStatus.VERIFIED :
                decision.equals("REJECTED") ? LotStatus.REJECTED : LotStatus.PENDING_VERIFICATION);
        lot = lotRepository.save(lot);
        if (decision.equals("VERIFIED") || decision.equals("REJECTED")) {
            notificationRepository.save(com.krishilink.entity.Notification.builder()
                    .user(lot.getOwner()).type("LOT_INSPECTION")
                    .message("Your " + lot.getCommodity() + " lot was " + decision.toLowerCase() + " by " + inspector.getName())
                    .read(false).build());
        }
        return ResponseEntity.ok(ApiResponse.ok(lot, "Inspection recorded"));
    }

    private static User requireInspector(User user) {
        if (user == null || (user.getRole() != Role.KRISHI_KENDRA && user.getRole() != Role.ADMIN)) {
            throw new BadRequestException("Only an authorized inspection centre can inspect lots");
        }
        return user;
    }

    private static Long longValue(Object value) {
        if (value == null) throw new BadRequestException("lotId is required");
        try { return Long.valueOf(value.toString()); }
        catch (NumberFormatException ex) { throw new BadRequestException("lotId must be numeric"); }
    }
    private static String text(Object value, String field) {
        if (value == null || value.toString().isBlank()) throw new BadRequestException(field + " is required");
        return value.toString().trim();
    }
    private static String textOr(Object value, String fallback) {
        return value == null || value.toString().isBlank() ? fallback : value.toString().trim();
    }
    private static String textOrNull(Object value) {
        return value == null || value.toString().isBlank() ? null : value.toString().trim();
    }
    private static Double number(Object value, Double fallback) {
        if (value == null || value.toString().isBlank()) return fallback;
        try { return Double.valueOf(value.toString()); }
        catch (NumberFormatException ex) { throw new BadRequestException("Inspection measurements must be numeric"); }
    }
}
