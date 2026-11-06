package com.krishilink.marketservice.dto;

import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class PriceDtos {

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceInsightDto {
        private String commodity;
        private Double currentPrice;
        private Double average7Days;
        private Double average30Days;
        private Double minPrice;
        private Double maxPrice;
        private Double changePercentage;
        private String trend; // UP, DOWN, STABLE
        private LocalDateTime lastUpdated;
        private List<PriceHistoryPoint> history;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceHistoryPoint {
        private String commodity;
        private Double modalPrice;
        private Double minPrice;
        private Double maxPrice;
        private LocalDate date;
        private Integer mandisReporting;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class MarketRecommendationDto {
        private Long id;
        private String _id;
        private String commodity;
        private String variety;
        private String grade;
        private LocalDate arrivalDate;
        private Double modalPrice;
        private Double modalPricePerQtl;
        private Double minPrice;
        private Double maxPrice;
        private Double netPrice;
        private Double netPricePerQtl;
        private Double distanceKm;
        private Double transportCostPerKm;
        private Double estimatedTransportCost;
        private Boolean isSameTown;
        private Boolean isSameDistrict;
        private String source;
        private Integer recommendationScore;
        private List<String> recommendationReasons;
        private String badge;
        private Boolean isNearest;
        private Boolean isRecommended;
        private Map<String, Object> market;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class SellAdviceDto {
        private String recommendation; // WAIT, SELL_NOW, COMPARE_MARKETS
        private Integer confidence;
        private String reason;
        private String disclaimer;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class PriceOpportunityAlertDto {
        private String commodity;
        private String recommendation; // SELL_NOW, HOLD_IN_STORAGE, MONITOR
        private String badgeColor;
        private String title;
        private String reasoning;
        private Double currentRate;
        private Double benchmarkRate;
        private Double premiumPercentage;
        private Double potentialGainPerQuintal;
        private String topMandiName;
        private Double topMandiPrice;
        private Double distanceKm;
    }
}
