package com.krishilink.lotservice.controller;

import com.krishilink.lotservice.dto.ApiResponse;
import com.krishilink.lotservice.entity.Lot;
import com.krishilink.lotservice.entity.Quality;
import com.krishilink.lotservice.enums.LotStatus;
import com.krishilink.lotservice.repository.LotRepository;
import com.krishilink.lotservice.repository.QualityRepository;
import com.krishilink.lotservice.security.UserPrincipal;
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

    @GetMapping("/lots")
    public ResponseEntity<ApiResponse<List<Lot>>> getInspectionLots(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String commodity) {
        List<Lot> lots = lotRepository.findAll();
        if (commodity != null && !commodity.isBlank()) {
            lots = lots.stream().filter(l -> l.getCommodity().equalsIgnoreCase(commodity)).toList();
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
                .orElseThrow(() -> new RuntimeException("Lot not found"));

        String inspectionStatus = body.getOrDefault("inspectionStatus", "VERIFIED").toString();
        String grade = body.getOrDefault("grade", "Grade A").toString();
        Double moisture = body.get("moisture") != null ? Double.valueOf(body.get("moisture").toString()) : 12.0;
        Double foreignMatter = body.get("foreignMatter") != null ? Double.valueOf(body.get("foreignMatter").toString()) : 1.0;
        Double damaged = body.get("damagedPercentage") != null ? Double.valueOf(body.get("damagedPercentage").toString()) : 0.5;

        Quality quality = lot.getQuality() != null ? lot.getQuality() : new Quality();
        quality.setGrade("REJECTED".equalsIgnoreCase(inspectionStatus) ? "Rejected" : grade);
        quality.setMoisture(moisture);
        quality.setForeignMatter(foreignMatter);
        quality.setDamagedPercentage(damaged);
        quality.setInspectionStatus(inspectionStatus);
        quality.setInspectedByName("ICAR - Krishi Vigyan Kendra Indore");
        quality.setInspectedAt(LocalDateTime.now());
        quality.setCertification("VERIFIED".equalsIgnoreCase(inspectionStatus) ? "Krishi Vigyan Kendra Certified" : "Rejected");
        quality = qualityRepository.save(quality);

        lot.setQuality(quality);
        lot.setStatus("VERIFIED".equalsIgnoreCase(inspectionStatus) ? LotStatus.VERIFIED : LotStatus.REJECTED);
        lot = lotRepository.save(lot);

        return ResponseEntity.ok(ApiResponse.ok(lot, "Inspection completed"));
    }
}
