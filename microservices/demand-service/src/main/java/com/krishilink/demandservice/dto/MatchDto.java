package com.krishilink.demandservice.dto;

import lombok.*;

import java.util.List;
import java.util.Map;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MatchDto {
    private Map<String, Object> lot;
    private Integer matchScore;
    private Map<String, Integer> breakdown;
    private List<String> reasons;
}
