package com.krishilink.dto;

import java.math.BigDecimal;

import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfferDto {
    private Long lotId;
    private Double quantity;
    private BigDecimal pricePerUnit;
    private BigDecimal totalAmount;
    private String message;
    private LocalDate validUntil;
}
