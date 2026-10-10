package com.krishilink.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.List;
import java.util.Map;

public class AuthDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class RegisterRequest {
        private String name;
        private String email;
        private String password;
        private String role;
        private String phone;
        private String location;
        private String address;
        private String district;
        private String state;
        private String pincode;
        private Double latitude;
        private Double longitude;

        // Farmer fields
        private String farmName;
        private Double landSize;
        private String primaryCrop;
        private List<String> crops;
        private Double availableQuantity;
        private String cropQuality;
        private String fpoAssociation;

        // Buyer / FPO fields
        private String organizationName;
        private String buyerType;
        private List<String> requiredCrops;
        private Double requiredQuantity;
        private String qualityRequirements;
        private Map<String, Object> qualitySpecs;
        private String registrationNumber;
        private Integer memberCount;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AuthResponse {
        private String token;
        private Object user;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class UpdateProfileRequest {
        private String name;
        private String phone;
        private String location;
        private String address;
        private String district;
        private String state;
        private String pincode;
        private String organizationName;
        private String buyerType;
        private String registrationNumber;
        private Integer memberCount;
        private String fpoLeaderDesignation;
        private Integer fpoEstablishmentYear;
        private String fpoIncorporationType;
        private String fpoAggregationCapacity;
        private String buyerCapacity;
        private String primaryCrop;
        private Object crops;
        private Double availableQuantity;
        private String cropQuality;
        private String fpoAssociation;
        private Object requiredCrops;
        private Double requiredQuantity;
        private String qualityRequirements;
        private Map<String, Object> qualitySpecs;
        private String profilePhoto;
        private String farmName;
        private Double landSize;
        private Boolean businessVerified;
        private String gstNumber;
        private String panNumber;
        private String mandiLicenseNumber;
        private Double latitude;
        private Double longitude;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KycOtpRequest {
        private String aadhaarNumber;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class KycVerifyRequest {
        @JsonProperty("client_id")
        private String clientId;
        private String otp;
    }
}
