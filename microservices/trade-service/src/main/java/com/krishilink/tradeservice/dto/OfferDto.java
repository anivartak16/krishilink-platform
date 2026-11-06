package com.krishilink.tradeservice.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OfferDto {
    private Long lotId;
    private Double quantity;
    private Double pricePerUnit;
    private String message;
}
