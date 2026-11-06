package com.krishilink.marketservice.config;

import com.krishilink.marketservice.entity.Market;
import com.krishilink.marketservice.entity.MarketPrice;
import com.krishilink.marketservice.entity.Warehouse;
import com.krishilink.marketservice.entity.LogisticsProvider;
import com.krishilink.marketservice.repository.MarketPriceRepository;
import com.krishilink.marketservice.repository.MarketRepository;
import com.krishilink.marketservice.repository.WarehouseRepository;
import com.krishilink.marketservice.repository.LogisticsProviderRepository;
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

    private final MarketRepository marketRepository;
    private final MarketPriceRepository marketPriceRepository;
    private final WarehouseRepository warehouseRepository;
    private final LogisticsProviderRepository logisticsProviderRepository;

    @Override
    public void run(String... args) {
        if (marketRepository.count() > 0) return;

        log.info("Initializing Market Service mandis, rates, and storage/logistics...");

        Market m1 = Market.builder().name("Indore Krishi Upaj Mandi").district("Indore").state("Madhya Pradesh").latitude(22.7196).longitude(75.8577).operatingHours("06:00 - 18:00").build();
        Market m2 = Market.builder().name("Dewas Mandi").district("Dewas").state("Madhya Pradesh").latitude(22.9676).longitude(76.0534).operatingHours("07:00 - 17:00").build();
        Market m3 = Market.builder().name("Ujjain APMC Mandi").district("Ujjain").state("Madhya Pradesh").latitude(23.1765).longitude(75.7885).operatingHours("06:30 - 17:30").build();
        marketRepository.saveAll(List.of(m1, m2, m3));

        MarketPrice p1 = MarketPrice.builder().market(m1).marketName("Indore Krishi Upaj Mandi").district("Indore").state("Madhya Pradesh").commodity("Wheat").variety("Lokwan").grade("FAQ").minPrice(2380.0).maxPrice(2560.0).modalPrice(2450.0).priceDate(LocalDate.now()).build();
        MarketPrice p2 = MarketPrice.builder().market(m1).marketName("Indore Krishi Upaj Mandi").district("Indore").state("Madhya Pradesh").commodity("Soybean").variety("Yellow").grade("FAQ").minPrice(4700.0).maxPrice(4980.0).modalPrice(4850.0).priceDate(LocalDate.now()).build();
        MarketPrice p3 = MarketPrice.builder().market(m2).marketName("Dewas Mandi").district("Dewas").state("Madhya Pradesh").commodity("Wheat").variety("Sharbati").grade("Grade A").minPrice(2450.0).maxPrice(2680.0).modalPrice(2550.0).priceDate(LocalDate.now()).build();
        marketPriceRepository.saveAll(List.of(p1, p2, p3));

        Warehouse w1 = Warehouse.builder().name("Indore Cold Chain & Agro Storage").type("Cold Storage").location("Sanwer Road Industrial Area").district("Indore").state("Madhya Pradesh").capacity(5000.0).availableSpace(2200.0).ratePerMonth(65.0).contactPhone("9876543220").facilities("Climate Controlled, Humidity Regulated, CCTV").build();
        warehouseRepository.save(w1);

        LogisticsProvider lp1 = LogisticsProvider.builder().name("KrishiLink Agri Logistics Express").vehicleTypes("32ft Multi-Axle, 14ft Open Truck").coverageArea("Madhya Pradesh, Maharashtra, Gujarat").ratePerKm(32.0).phone("9876543230").capacityTons(25.0).rating(4.8).build();
        logisticsProviderRepository.save(lp1);

        log.info("Market Service initialized with demo operational data!");
    }
}
