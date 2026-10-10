package com.krishilink.dto;

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
    private Double pricePerUnit;
    private Double totalAmount;
    private String message;
    private LocalDate validUntil;
}
