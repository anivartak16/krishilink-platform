package com.krishilink.dto;

import com.krishilink.entity.Lot;
import lombok.*;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchDto {
    private Lot lot;
    private Integer matchScore;
    private Boolean isUserLot;
    private Boolean isFpoLot;
    private Double netRealisation;
    private List<String> reasons;
    private Map<String, Integer> breakdown;
}
