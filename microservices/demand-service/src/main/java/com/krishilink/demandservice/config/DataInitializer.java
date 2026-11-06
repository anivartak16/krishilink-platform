package com.krishilink.demandservice.config;

import com.krishilink.demandservice.entity.Demand;
import com.krishilink.demandservice.enums.DemandStatus;
import com.krishilink.demandservice.repository.DemandRepository;
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

    private final DemandRepository demandRepository;

    @Override
    public void run(String... args) {
        if (demandRepository.count() > 0) return;

        log.info("Initializing Demand Service demo procurement demands...");

        Demand d1 = Demand.builder()
                .buyerId(3L)
                .buyerName("AgroCorp Direct")
                .buyerOrganization("AgroCorp Foods Pvt Ltd")
                .buyerPhone("9876543213")
                .commodity("Wheat")
                .requiredQuantity(5000.0)
                .unit("KG")
                .requiredQuality("Grade A")
                .maxMoisture(12.0)
                .maxPrice(2500.0)
                .preferredLocation("Indore")
                .paymentTerms("100% Escrow Secured")
                .deadline(LocalDate.now().plusDays(20))
                .status(DemandStatus.ACTIVE)
                .build();

        Demand d2 = Demand.builder()
                .buyerId(3L)
                .buyerName("AgroCorp Direct")
                .buyerOrganization("AgroCorp Foods Pvt Ltd")
                .buyerPhone("9876543213")
                .commodity("Soybean")
                .requiredQuantity(3000.0)
                .unit("KG")
                .requiredQuality("Grade A")
                .maxMoisture(11.0)
                .maxPrice(4900.0)
                .preferredLocation("Dewas")
                .paymentTerms("100% Escrow Secured")
                .deadline(LocalDate.now().plusDays(15))
                .status(DemandStatus.ACTIVE)
                .build();

        demandRepository.saveAll(List.of(d1, d2));
        log.info("Demand Service initialized with demo demands!");
    }
}
