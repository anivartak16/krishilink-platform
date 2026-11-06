package com.krishilink.userservice.config;

import com.krishilink.userservice.entity.User;
import com.krishilink.userservice.enums.Role;
import com.krishilink.userservice.enums.VerificationStatus;
import com.krishilink.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        if (userRepository.count() > 0) {
            ensureKvkUser();
            return;
        }

        log.info("Initializing User Service database with demo users...");
        String encodedPass = passwordEncoder.encode("Demo@12345");

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

        User buyer = User.builder()
                .name("AgroCorp Direct")
                .email("buyer@krishilink.com")
                .password(encodedPass)
                .role(Role.BUYER)
                .phone("9876543213")
                .location("Indore")
                .district("Indore")
                .state("Madhya Pradesh")
                .organizationName("AgroCorp Foods Pvt Ltd")
                .buyerType("Institutional Buyer")
                .buyerCapacity(50000.0)
                .preferredCommodities("Wheat,Soybean")
                .verification(VerificationStatus.VERIFIED)
                .verificationBadge("VERIFIED_BUYER")
                .businessVerified(true)
                .kycVerified(true)
                .build();

        User fpo = User.builder()
                .name("Malwa Kisan Producer Company")
                .email("fpo@krishilink.com")
                .password(encodedPass)
                .role(Role.FPO)
                .phone("9876543214")
                .location("Indore")
                .district("Indore")
                .state("Madhya Pradesh")
                .organizationName("Malwa Kisan FPO")
                .memberCount(450)
                .fpoAggregationCapacity(12000.0)
                .fpoLeaderDesignation("Managing Director")
                .fpoIncorporationType("Farmer Producer Company")
                .fpoEstablishmentYear(2021)
                .verification(VerificationStatus.VERIFIED)
                .verificationBadge("VERIFIED_FPO")
                .build();

        User kvk = User.builder()
                .name("Krishi Vigyan Kendra")
                .email("kvk@krishilink.com")
                .password(encodedPass)
                .role(Role.KRISHI_KENDRA)
                .phone("9876543219")
                .location("Indore")
                .district("Indore")
                .state("Madhya Pradesh")
                .organizationName("ICAR - Krishi Vigyan Kendra Indore")
                .verification(VerificationStatus.VERIFIED)
                .verificationBadge("KVK_CERTIFIED")
                .build();

        userRepository.saveAll(java.util.List.of(admin, farmer, buyer, fpo, kvk));
        log.info("User Service initialized with demo users!");
    }

    private void ensureKvkUser() {
        if (!userRepository.existsByEmailIgnoreCase("kvk@krishilink.com")) {
            User kvk = User.builder()
                    .name("Krishi Vigyan Kendra")
                    .email("kvk@krishilink.com")
                    .password(passwordEncoder.encode("Demo@12345"))
                    .role(Role.KRISHI_KENDRA)
                    .phone("9876543219")
                    .location("Indore")
                    .district("Indore")
                    .state("Madhya Pradesh")
                    .organizationName("ICAR - Krishi Vigyan Kendra Indore")
                    .verification(VerificationStatus.VERIFIED)
                    .verificationBadge("KVK_CERTIFIED")
                    .build();
            userRepository.save(kvk);
        }
    }
}
