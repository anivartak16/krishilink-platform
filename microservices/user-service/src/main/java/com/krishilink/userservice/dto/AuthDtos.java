package com.krishilink.userservice.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.krishilink.userservice.enums.Role;
import lombok.*;

import java.util.List;
import java.util.Map;

public class AuthDtos {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class LoginRequest {
        private String email;
        private String password;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class RegisterRequest {
        private String name;
        private String email;
        private String password;
        private Role role;
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
        private Object crops;
        private Double availableQuantity;
        private String organizationName;
        private String buyerType;
        private String preferredCommodities;
        private Object requiredCrops;
        private Double requiredQuantity;
        private String qualityRequirements;
        private String fpoAssociation;
        private Integer memberCount;
        private String panNumber;
        private String gstNumber;
        private String registrationNumber;
        private String mandiLicenseNumber;
        private Map<String, Object> qualitySpecs;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class UpdateProfileRequest {
        private String name;
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
        private Object crops;
        private Double availableQuantity;
        private String cropQuality;
        private String organizationName;
        private String buyerType;
        private Double buyerCapacity;
        private String preferredCommodities;
        private Object requiredCrops;
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
        private String profilePhoto;
        private Map<String, Object> qualitySpecs;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class AuthResponse {
        private String token;
        private Map<String, Object> user;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class KycOtpRequest {
        private String aadhaarNumber;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
    public static class KycVerifyRequest {
        @JsonProperty("client_id")
        @JsonAlias("clientId")
        private String clientId;
        private String otp;
    }
}
