package com.krishilink.config;

import com.krishilink.entity.*;
import com.krishilink.enums.*;
import com.krishilink.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final MarketRepository marketRepository;
    private final MarketPriceRepository marketPriceRepository;
    private final QualityRepository qualityRepository;
    private final LotRepository lotRepository;
    private final DemandRepository demandRepository;
    private final OfferRepository offerRepository;
    private final TransactionRepository transactionRepository;
    private final PaymentRepository paymentRepository;
    private final WarehouseRepository warehouseRepository;
    private final LogisticsProviderRepository logisticsProviderRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            log.info("Database already initialized. Skipping seed.");
            return;
        }

        log.info("Initializing KrishiLink database with demo data...");
        String encodedPass = passwordEncoder.encode("Demo@12345");

        // 1. Users
        User admin = User.builder()
                .name("Admin")
                .email("admin@krishilink.com")
                .password(encodedPass)
                .role(Role.ADMIN)
                .phone("9876543210")
                .location("Bhopal")
                .district("Bhopal")
                .state("Madhya Pradesh")
                .latitude(23.2599)
                .longitude(77.4126)
                .verification(VerificationStatus.VERIFIED)
                .verificationBadge("ADMIN_VERIFIED")
                .build();

        User farmer = User.builder()
                .name("Ramesh Patel")
                .email("farmer@krishilink.com")
                .password(encodedPass)
                .role(Role.FARMER)
                .phone("9876543211")
                .location("Indore")
                .district("Indore")
                .state("Madhya Pradesh")
                .latitude(22.7196)
                .longitude(75.8577)
                .farmName("Patel Krishi Farm")
                .landSize(12.5)
                .primaryCrop("Wheat")
                .crops("Wheat,Soybean,Gram")
                .availableQuantity(2500.0)
                .verification(VerificationStatus.VERIFIED)
                .verificationBadge("EKYC_VERIFIED_FARMER")
                .kycVerified(true)
                .kycVerifiedAt(LocalDateTime.now())
                .aadhaarLast4("8834")
                .ekycStatus("VERIFIED")
                .build();

        User farmer2 = User.builder()
                .name("Sita Verma")
                .email("farmer2@krishilink.com")
                .password(encodedPass)
                .role(Role.FARMER)
                .phone("9876543212")
                .location("Dewas")
                .district("Dewas")
                .state("Madhya Pradesh")
                .latitude(22.9676)
                .longitude(76.0534)
                .farmName("Verma Organic Farms")
                .landSize(8.0)
                .primaryCrop("Soybean")
                .crops("Soybean,Wheat")
                .availableQuantity(1800.0)
                .verification(VerificationStatus.VERIFIED)
                .verificationBadge("EKYC_VERIFIED_FARMER")
                .kycVerified(true)
                .aadhaarLast4("4412")
                .build();

        User fpo = User.builder()
                .name("Narmada FPO")
                .email("fpo@krishilink.com")
                .password(encodedPass)
                .role(Role.FPO)
                .phone("9876543213")
                .location("Ujjain")
                .district("Ujjain")
                .state("Madhya Pradesh")
                .latitude(23.1765)
                .longitude(75.7885)
                .organizationName("Narmada Farmers Producer Company")
                .registrationNumber("FPO-MP-2021-089")
                .memberCount(450)
                .fpoAggregationCapacity("2,500 MT / Season")
                .verification(VerificationStatus.VERIFIED)
                .verificationBadge("VERIFIED_FPO")
                .build();

        User buyer = User.builder()
                .name("ABC Foods Ltd")
                .email("buyer@krishilink.com")
                .password(encodedPass)
                .role(Role.BUYER)
                .phone("9876543214")
                .location("Indore")
                .district("Indore")
                .state("Madhya Pradesh")
                .latitude(22.7196)
                .longitude(75.8577)
                .organizationName("ABC Agro & Foods Processing Ltd")
                .buyerType("Food Processor & Exporter")
                .buyerCapacity("1,200 MT / Month")
                .gstNumber("23AABCC1234F1Z8")
                .panNumber("AABCC1234F")
                .verification(VerificationStatus.VERIFIED)
                .verificationBadge("VERIFIED_BUYER")
                .build();

        User buyer2 = User.builder()
                .name("Malwa Traders")
                .email("buyer2@krishilink.com")
                .password(encodedPass)
                .role(Role.BUYER)
                .phone("9876543215")
                .location("Bhopal")
                .district("Bhopal")
                .state("Madhya Pradesh")
                .latitude(23.2599)
                .longitude(77.4126)
                .organizationName("Malwa Grain Traders")
                .buyerType("Wholesale Grain Trader")
                .verification(VerificationStatus.VERIFIED)
                .verificationBadge("VERIFIED_BUYER")
                .build();

        userRepository.saveAll(List.of(admin, farmer, farmer2, fpo, buyer, buyer2));

        // 2. Mandis
        Map<String, List<Double>> mandis = Map.of(
                "Indore Mandi", List.of(75.8577, 22.7196),
                "Dewas Mandi", List.of(76.0534, 22.9676),
                "Ujjain Mandi", List.of(75.7885, 23.1765),
                "Bhopal Mandi", List.of(77.4126, 23.2599),
                "Sagar Mandi", List.of(78.7378, 23.8388),
                "Vidisha Mandi", List.of(77.8146, 23.5236),
                "Mandsaur Mandi", List.of(75.0693, 24.0734),
                "Neemuch Mandi", List.of(74.8720, 24.4764),
                "Ratlam Mandi", List.of(75.0376, 23.3315),
                "Khandwa Mandi", List.of(76.3498, 21.8247)
        );

        int mi = 0;
        for (var entry : mandis.entrySet()) {
            String mName = entry.getKey();
            String dist = mName.replace(" Mandi", "");
            Market m = Market.builder()
                    .name(mName)
                    .location(dist)
                    .district(dist)
                    .state("Madhya Pradesh")
                    .commodities("Wheat,Soybean,Gram,Onion,Potato")
                    .longitude(entry.getValue().get(0))
                    .latitude(entry.getValue().get(1))
                    .transportCostPerKm(8.0)
                    .reviewAverage(4.4)
                    .reviewCount(45 + mi * 5)
                    .build();
            m = marketRepository.save(m);

            // Seed price trends (last 10 days)
            for (int d = 0; d < 10; d++) {
                LocalDate date = LocalDate.now().minusDays(d);
                double baseWheat = 2450.0 + (mi * 15) - (d * 5);
                MarketPrice mpWheat = MarketPrice.builder()
                        .market(m)
                        .marketName(mName)
                        .district(dist)
                        .state("Madhya Pradesh")
                        .commodity("Wheat")
                        .variety("Sharbati")
                        .grade("Grade A")
                        .priceDate(date)
                        .minPrice(baseWheat - 80)
                        .maxPrice(baseWheat + 100)
                        .modalPrice(baseWheat)
                        .arrivalVolume(150.0 + mi * 20)
                        .unit("KG")
                        .source("AGMARKNET")
                        .build();

                double baseSoy = 4800.0 + (mi * 20) - (d * 8);
                MarketPrice mpSoy = MarketPrice.builder()
                        .market(m)
                        .marketName(mName)
                        .district(dist)
                        .state("Madhya Pradesh")
                        .commodity("Soybean")
                        .variety("Yellow")
                        .grade("FAQ")
                        .priceDate(date)
                        .minPrice(baseSoy - 100)
                        .maxPrice(baseSoy + 120)
                        .modalPrice(baseSoy)
                        .arrivalVolume(90.0 + mi * 10)
                        .unit("KG")
                        .source("AGMARKNET")
                        .build();

                marketPriceRepository.saveAll(List.of(mpWheat, mpSoy));
            }
            mi++;
        }

        // 3. Qualities
        Quality q1 = Quality.builder().grade("Grade A").moisture(11.0).foreignMatter(0.5).damagedPercentage(1.0).certification("Demo inspected").build();
        Quality q2 = Quality.builder().grade("Grade B").moisture(13.0).foreignMatter(1.2).damagedPercentage(2.0).certification("Standard verified").build();
        qualityRepository.saveAll(List.of(q1, q2));

        // 4. Lots
        Lot lot1 = Lot.builder()
                .owner(farmer)
                .ownerType("FARMER")
                .commodity("Wheat")
                .quantity(2500.0)
                .remainingQuantity(1500.0)
                .expectedPrice(24.5)
                .location("Indore")
                .longitude(75.8577)
                .latitude(22.7196)
                .quality(q1)
                .harvestDate(LocalDate.now().minusDays(10))
                .availableUntil(LocalDate.now().plusDays(20))
                .farmingType("conventional")
                .status(LotStatus.PARTIALLY_SOLD)
                .build();

        Lot lot2 = Lot.builder()
                .owner(farmer2)
                .ownerType("FARMER")
                .commodity("Soybean")
                .quantity(1800.0)
                .remainingQuantity(1800.0)
                .expectedPrice(48.0)
                .location("Dewas")
                .longitude(76.0534)
                .latitude(22.9676)
                .quality(q1)
                .harvestDate(LocalDate.now().minusDays(7))
                .availableUntil(LocalDate.now().plusDays(25))
                .farmingType("organic")
                .organicCertificationStatus("verified")
                .certificationType("PGS-India")
                .status(LotStatus.AVAILABLE)
                .build();

        Lot lot3 = Lot.builder()
                .owner(fpo)
                .ownerType("FPO")
                .commodity("Wheat")
                .quantity(5000.0)
                .remainingQuantity(5000.0)
                .expectedPrice(24.2)
                .location("Ujjain")
                .longitude(75.7885)
                .latitude(23.1765)
                .quality(q2)
                .availableUntil(LocalDate.now().plusDays(30))
                .farmingType("conventional")
                .status(LotStatus.AVAILABLE)
                .build();

        lotRepository.saveAll(List.of(lot1, lot2, lot3));

        // 5. Demands
        Demand dem1 = Demand.builder()
                .buyer(buyer)
                .commodity("Wheat")
                .requiredQuantity(3000.0)
                .requiredQuality("Grade A")
                .preferredLocation("Indore")
                .maxPrice(26.0)
                .deadline(LocalDate.now().plusDays(14))
                .status(DemandStatus.ACTIVE)
                .build();

        Demand dem2 = Demand.builder()
                .buyer(buyer2)
                .commodity("Soybean")
                .requiredQuantity(1500.0)
                .requiredQuality("Grade A")
                .preferredLocation("Dewas")
                .maxPrice(50.0)
                .farmingType("organic")
                .deadline(LocalDate.now().plusDays(10))
                .status(DemandStatus.ACTIVE)
                .build();

        demandRepository.saveAll(List.of(dem1, dem2));

        // 6. Offer
        Offer offer1 = Offer.builder()
                .lot(lot1)
                .buyer(buyer)
                .quantity(1000.0)
                .pricePerUnit(25.2)
                .totalAmount(25200.0)
                .message("Pickup arranged within two days via logistics provider.")
                .validUntil(LocalDate.now().plusDays(5))
                .status(OfferStatus.ACCEPTED)
                .build();

        offerRepository.save(offer1);

        // 7. Transaction & Payment
        Transaction tx1 = Transaction.builder()
                .offer(offer1)
                .lot(lot1)
                .buyer(buyer)
                .seller(farmer)
                .quantity(1000.0)
                .amount(25200.0)
                .status(TransactionStatus.IN_TRANSIT)
                .eventsJson("[{\"status\":\"CREATED\",\"note\":\"Offer accepted and trade created\"},{\"status\":\"CONFIRMED\",\"note\":\"Seller confirmed dispatch\"},{\"status\":\"IN_TRANSIT\",\"note\":\"Pickup vehicle assigned\"}]")
                .build();

        tx1 = transactionRepository.save(tx1);

        Payment pay1 = Payment.builder()
                .transaction(tx1)
                .amount(25200.0)
                .paymentMethod("Bank Transfer")
                .status(PaymentStatus.PROCESSING)
                .remarks("Escrow release after delivery")
                .build();

        paymentRepository.save(pay1);

        // 8. Notifications
        Notification n1 = Notification.builder()
                .user(farmer)
                .type("TRANSACTION_CREATED")
                .message("ABC Foods Ltd transaction is in transit (1,000 kg Wheat).")
                .read(false)
                .build();
        notificationRepository.save(n1);

        // 9. Warehouses & Logistics
        Warehouse w1 = Warehouse.builder()
                .name("Indore Cold & Grain Storage")
                .location("Indore")
                .availableCapacity(500.0)
                .storageType("Dry Warehouse")
                .pricePerUnitPerDay(2.5)
                .facilities("CCTV,Weighbridge,Insurance")
                .verificationStatus("VERIFIED")
                .build();
        Warehouse w2 = Warehouse.builder()
                .name("Malwa Agro Silo")
                .location("Dewas")
                .availableCapacity(1200.0)
                .storageType("Silo")
                .pricePerUnitPerDay(2.0)
                .facilities("Automated Aeration,Moisture Control")
                .verificationStatus("VERIFIED")
                .build();
        warehouseRepository.saveAll(List.of(w1, w2));

        LogisticsProvider lp1 = LogisticsProvider.builder()
                .name("Kisan Express Transport")
                .phone("9876543210")
                .vehicleType("10-Ton Truck")
                .capacity(10000.0)
                .serviceAreas("Indore,Dewas,Ujjain,Bhopal")
                .pricePerKm(28.0)
                .availability(true)
                .build();
        LogisticsProvider lp2 = LogisticsProvider.builder()
                .name("Malwa Logistics")
                .phone("9876500000")
                .vehicleType("Mini Truck (Pick-up)")
                .capacity(3000.0)
                .serviceAreas("Bhopal,Sehore,Indore")
                .pricePerKm(22.0)
                .availability(true)
                .build();
        logisticsProviderRepository.saveAll(List.of(lp1, lp2));

        log.info("Database initialization completed successfully! All demo credentials ready.");
    }
}
