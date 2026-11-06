package com.krishilink.userservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.krishilink.userservice.enums.Role;
import com.krishilink.userservice.enums.VerificationStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

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

    private String farmName;
    private Double landSize;
    private String primaryCrop;
    private String crops;
    private Double availableQuantity;
    private String cropQuality;

    private String organizationName;
    private String buyerType;
    private Double buyerCapacity;
    private String preferredCommodities;
    private String requiredCrops;
    private Double requiredQuantity;
    private String qualityRequirements;

    private String fpoAssociation;
    private Integer memberCount;
    private Double fpoAggregationCapacity;
    private String fpoLeaderDesignation;
    private String fpoIncorporationType;
    private Integer fpoEstablishmentYear;

    private String panNumber;
    private String gstNumber;
    private String registrationNumber;
    private String mandiLicenseNumber;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private VerificationStatus verification = VerificationStatus.PENDING;

    private String verificationBadge;
    private Boolean businessVerified;
    private Boolean kycVerified;
    private LocalDateTime kycVerifiedAt;
    private String aadhaarLast4;

    private String ekycStatus;
    private String ekycType;
    private String ekycIdNumber;
    private LocalDateTime ekycVerifiedAt;

    @Column(columnDefinition = "TEXT")
    private String ekycDetails;

    @Column(columnDefinition = "TEXT")
    private String qualitySpecsJson;

    private Double tradeRating;
    private String profilePhoto;

    @Builder.Default
    private Boolean active = true;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @JsonProperty("_id")
    public String getMongoStyleId() {
        return id != null ? id.toString() : null;
    }
}
