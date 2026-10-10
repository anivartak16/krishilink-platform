package com.krishilink.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.krishilink.enums.Role;
import com.krishilink.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.FARMER;

    private String phone;
    private String location;
    private String address;
    private String district;
    private String state;
    private String pincode;

    private Double latitude;
    private Double longitude;

    // Farmer specific fields
    private String farmName;
    private Double landSize;
    private String primaryCrop;

    @Column(columnDefinition = "TEXT")
    private String crops; // Comma-separated or JSON list

    private Double availableQuantity;

    @Builder.Default
    private String cropQuality = "Grade A";

    private String fpoAssociation;

    // Buyer / FPO specific fields
    private String organizationName;
    private String buyerType;

    @Column(columnDefinition = "TEXT")
    private String requiredCrops;

    private Double requiredQuantity;
    private String qualityRequirements;

    @Column(columnDefinition = "TEXT")
    private String qualitySpecsJson;

    private String registrationNumber;
    private Integer memberCount;

    @Builder.Default
    private String fpoLeaderDesignation = "Chairman & Managing Director";

    @Builder.Default
    private Integer fpoEstablishmentYear = 2021;

    @Builder.Default
    private String fpoIncorporationType = "Farmer Producer Company (Companies Act)";

    @Builder.Default
    private String fpoAggregationCapacity = "1,500 MT / Season";

    @Builder.Default
    private String buyerCapacity = "500 MT / Month";

    @Column(columnDefinition = "TEXT")
    private String preferredCommodities;

    // Verification & KYC
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VerificationStatus verification = VerificationStatus.UNVERIFIED;

    private String gstNumber;
    private String panNumber;
    private String mandiLicenseNumber;
    private String verificationBadge;

    @Builder.Default
    private Double tradeRating = 4.5;

    @Builder.Default
    private Boolean kycVerified = false;

    private LocalDateTime kycVerifiedAt;
    private String aadhaarLast4;

    @Builder.Default
    private String ekycStatus = "NOT_STARTED";
    private String ekycType;
    private String ekycIdNumber;
    private LocalDateTime ekycVerifiedAt;

    @Column(columnDefinition = "TEXT")
    private String ekycDetails;

    @Column(columnDefinition = "LONGTEXT")
    private String profilePhoto;

    @Builder.Default
    private Boolean businessVerified = false;

    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    // Helper method to get formatted crops array for JSON responses
    @JsonProperty("crops")
    public List<String> getCropsList() {
        if (crops == null || crops.isBlank()) {
            return primaryCrop != null ? List.of(primaryCrop) : new ArrayList<>();
        }
        return Arrays.stream(crops.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @JsonProperty("requiredCrops")
    public List<String> getRequiredCropsList() {
        if (requiredCrops == null || requiredCrops.isBlank()) {
            return new ArrayList<>();
        }
        return Arrays.stream(requiredCrops.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    // Helper for mongo compatibility in React frontend
    @JsonProperty("_id")
    public String getMongoStyleId() {
        return id != null ? id.toString() : null;
    }
}
