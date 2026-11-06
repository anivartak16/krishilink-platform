package com.krishilink.demandservice.dto;

import lombok.*;

import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DemandDto {
    private String commodity;
    private Double requiredQuantity;
    private String unit;
    private String requiredQuality;
    private String variety;
    private Double maxMoisture;
    private String preferredLocation;
    private String deliveryLocation;
    private Double maxPrice;
    private String paymentTerms;
    private String contactPhone;
    private String farmingType;
    private String notes;
    private LocalDate deadline;
}
