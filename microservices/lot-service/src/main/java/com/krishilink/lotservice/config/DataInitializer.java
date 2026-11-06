package com.krishilink.lotservice.config;

import com.krishilink.lotservice.entity.Lot;
import com.krishilink.lotservice.entity.Quality;
import com.krishilink.lotservice.enums.LotStatus;
import com.krishilink.lotservice.repository.LotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final LotRepository lotRepository;

    @Override
    public void run(String... args) {
        if (lotRepository.count() > 0) return;

        log.info("Initializing Lot Service demo produce lots...");

        Quality q1 = Quality.builder()
                .grade("Grade A")
                .moisture(11.2)
                .foreignMatter(0.5)
                .damagedPercentage(0.8)
                .certification("Krishi Vigyan Kendra Certified (ICAR - KVK Indore)")
                .inspectionStatus("VERIFIED")
                .build();

        Lot lot1 = Lot.builder()
                .ownerId(2L)
                .ownerName("Ramesh Patel")
                .ownerPhone("9876543211")
                .ownerType("FARMER")
                .commodity("Wheat")
                .quantity(2500.0)
                .remainingQuantity(2500.0)
                .unit("KG")
                .expectedPrice(2450.0)
                .location("Indore")
                .longitude(75.8577)
                .latitude(22.7196)
                .farmingType("conventional")
                .organicCertificationStatus("verified")
                .quality(q1)
                .status(LotStatus.AVAILABLE)
                .availableUntil(LocalDate.now().plusDays(45))
                .build();

        Quality q2 = Quality.builder()
                .grade("Grade A")
                .moisture(10.5)
                .foreignMatter(0.3)
                .damagedPercentage(0.4)
                .certification("PGS-India Certified Organic")
                .inspectionStatus("VERIFIED")
                .build();

        Lot lot2 = Lot.builder()
                .ownerId(2L)
                .ownerName("Ramesh Patel")
                .ownerPhone("9876543211")
                .ownerType("FARMER")
                .commodity("Soybean")
                .quantity(1800.0)
                .remainingQuantity(1800.0)
                .unit("KG")
                .expectedPrice(4850.0)
                .location("Indore")
                .longitude(75.8577)
                .latitude(22.7196)
                .farmingType("organic")
                .organicCertificationStatus("verified")
                .certificationType("PGS-India")
                .certificateId("MP-IND-2024-ORG-778")
                .quality(q2)
                .status(LotStatus.AVAILABLE)
                .availableUntil(LocalDate.now().plusDays(30))
                .build();

        lotRepository.saveAll(List.of(lot1, lot2));
        log.info("Lot Service initialized with demo lots!");
    }
}
