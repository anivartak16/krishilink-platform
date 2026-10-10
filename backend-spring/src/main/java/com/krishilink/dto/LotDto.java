package com.krishilink.dto;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LotDto {
    private String commodity;
    private Double quantity;
    private Double remainingQuantity;
    private String unit;
    private LocalDate harvestDate;
    private String location;
    private Double latitude;
    private Double longitude;
    private Double expectedPrice;
    private String grade;
    private Double moisture;
    private Double foreignMatter;
    private Double damagedPercentage;
    private String certification;
    private LocalDate availableUntil;
    private String farmingType;
    private String organicCertificationStatus;
    private String certificationType;
    private String certificateId;
    private LocalDate certificateValidity;
    private String certificateDocumentUrl;
    private String ownerType;
}
