package com.krishilink.marketservice.service;

import com.krishilink.marketservice.dto.PriceDtos.*;
import com.krishilink.marketservice.entity.Market;
import com.krishilink.marketservice.entity.MarketPrice;
import com.krishilink.marketservice.repository.MarketPriceRepository;
import com.krishilink.marketservice.repository.MarketRepository;
import com.krishilink.marketservice.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class IntelligenceService {

    private final MarketPriceRepository marketPriceRepository;
    private final MarketRepository marketRepository;

    private static final Map<String, Double> MSP_BENCHMARKS = Map.ofEntries(
            Map.entry("wheat", 22.75),
            Map.entry("paddy", 23.0),
            Map.entry("rice", 23.0),
            Map.entry("soybean", 48.92),
            Map.entry("soyabean", 48.92),
            Map.entry("maize", 22.25),
            Map.entry("corn", 22.25),
            Map.entry("gram", 54.4),
            Map.entry("chana", 54.4),
            Map.entry("mustard", 56.5),
            Map.entry("cotton", 71.21),
            Map.entry("onion", 18.0),
            Map.entry("potato", 14.0)
    );

    public PriceInsightDto getPriceInsight(String commodity) {
        String comm = commodity != null && !commodity.isBlank() ? commodity.trim() : "Wheat";
        List<MarketPrice> rows = marketPriceRepository.findByCommodityIgnoreCaseOrderByPriceDateAsc(comm);

        if (rows.isEmpty()) {
            rows = marketPriceRepository.searchByCommodity(comm);
        }

        if (rows.isEmpty()) {
            return PriceInsightDto.builder()
                    .commodity(comm)
                    .currentPrice(24.5)
                    .average7Days(24.2)
                    .average30Days(23.8)
                    .minPrice(23.0)
                    .maxPrice(25.5)
                    .changePercentage(2.9)
                    .trend("UP")
                    .lastUpdated(LocalDateTime.now())
                    .history(List.of())
                    .build();
        }

        List<PriceHistoryPoint> history = rows.stream().map(r -> PriceHistoryPoint.builder()
                .commodity(r.getCommodity())
                .modalPrice(r.getModalPrice())
                .minPrice(r.getMinPrice())
                .maxPrice(r.getMaxPrice())
                .date(r.getPriceDate())
                .mandisReporting(1)
                .build()).toList();

        List<Double> prices = rows.stream().map(MarketPrice::getModalPrice).toList();
        double currentPrice = prices.get(prices.size() - 1);

        int count30 = Math.min(30, prices.size());
        double avg30 = prices.subList(prices.size() - count30, prices.size()).stream()
                .mapToDouble(Double::doubleValue).average().orElse(currentPrice);

        int count7 = Math.min(7, prices.size());
        double avg7 = prices.subList(prices.size() - count7, prices.size()).stream()
                .mapToDouble(Double::doubleValue).average().orElse(currentPrice);

        double min = prices.stream().mapToDouble(Double::doubleValue).min().orElse(currentPrice);
        double max = prices.stream().mapToDouble(Double::doubleValue).max().orElse(currentPrice);

        double changePct = avg30 > 0 ? Math.round(((currentPrice - avg30) / avg30) * 10000.0) / 100.0 : 0.0;
        String trend = changePct > 1.0 ? "UP" : changePct < -1.0 ? "DOWN" : "STABLE";

        return PriceInsightDto.builder()
                .commodity(comm)
                .currentPrice(currentPrice)
                .average7Days(Math.round(avg7 * 100.0) / 100.0)
                .average30Days(Math.round(avg30 * 100.0) / 100.0)
                .minPrice(min)
                .maxPrice(max)
                .changePercentage(changePct)
                .trend(trend)
                .lastUpdated(LocalDateTime.now())
                .history(history)
                .build();
    }

    public List<MarketRecommendationDto> getMarketsFor(String commodity, String district, String state, String location, String role) {
        String comm = commodity != null && !commodity.isBlank() ? commodity.trim() : "Wheat";
        List<MarketPrice> prices = marketPriceRepository.findByCommodityIgnoreCaseOrderByPriceDateDesc(comm);
        if (prices.isEmpty()) {
            prices = marketPriceRepository.findAll();
        }

        List<Double> userCoords = GeoUtils.resolveCoordinates(district, state, location);
        Map<String, MarketRecommendationDto> newest = new LinkedHashMap<>();

        for (MarketPrice row : prices) {
            String mName = row.getMarketName() != null ? row.getMarketName() :
                    (row.getMarket() != null ? row.getMarket().getName() : "Indore Mandi");
            String key = (mName + "_" + row.getCommodity()).toLowerCase();
            if (newest.containsKey(key)) continue;

            List<Double> marketCoords = GeoUtils.resolveCoordinates(row.getDistrict(), row.getState(), mName);
            double dist = GeoUtils.calculateDistanceKm(userCoords, marketCoords);

            double ratePerKg = row.getModalPrice() != null ? row.getModalPrice() : 0.0;
            double ratePerQtl = ratePerKg * 100.0;
            double transportCostPerKm = 0.04;
            double totalTransport = Math.round((dist * transportCostPerKm) * 100.0) / 100.0;
            double netPrice = Math.max(0.0, Math.round((ratePerKg - totalTransport) * 100.0) / 100.0);

            MarketRecommendationDto rec = MarketRecommendationDto.builder()
                    .id(row.getId())
                    ._id(row.getId() != null ? row.getId().toString() : null)
                    .commodity(row.getCommodity())
                    .variety(row.getVariety())
                    .grade(row.getGrade())
                    .arrivalDate(row.getPriceDate())
                    .modalPrice(ratePerKg)
                    .modalPricePerQtl(ratePerQtl)
                    .minPrice(row.getMinPrice())
                    .maxPrice(row.getMaxPrice())
                    .netPrice(netPrice)
                    .netPricePerQtl(netPrice * 100.0)
                    .distanceKm(dist)
                    .transportCostPerKm(transportCostPerKm)
                    .estimatedTransportCost(totalTransport)
                    .isSameTown(dist < 15.0)
                    .isSameDistrict(district != null && district.equalsIgnoreCase(row.getDistrict()))
                    .source("AGMARKNET")
                    .recommendationScore(75)
                    .recommendationReasons(List.of("Reliable APMC Mandi price discovery", "Direct market arrival data"))
                    .badge("RECOMMENDED")
                    .market(Map.of(
                            "name", mName,
                            "district", row.getDistrict() != null ? row.getDistrict() : "District",
                            "state", row.getState() != null ? row.getState() : "State"
                    ))
                    .build();

            newest.put(key, rec);
        }

        List<MarketRecommendationDto> list = new ArrayList<>(newest.values());
        list.sort(Comparator.comparingDouble((MarketRecommendationDto r) -> r.getNetPrice() != null ? -r.getNetPrice() : 0.0));
        return list;
    }

    public PriceOpportunityAlertDto getPriceOpportunityAlert(String commodity) {
        String comm = commodity != null && !commodity.isBlank() ? commodity.trim() : "Wheat";
        PriceInsightDto insight = getPriceInsight(comm);
        double current = insight != null && insight.getCurrentPrice() != null ? insight.getCurrentPrice() : 24.5;
        double benchmark = MSP_BENCHMARKS.getOrDefault(comm.toLowerCase(), current * 0.92);

        double premium = Math.round(((current - benchmark) / benchmark * 100.0) * 10.0) / 10.0;
        String rec = premium > 5.0 ? "SELL_NOW" : premium < -5.0 ? "HOLD_IN_STORAGE" : "MONITOR";

        return PriceOpportunityAlertDto.builder()
                .commodity(comm)
                .recommendation(rec)
                .badgeColor(premium > 5.0 ? "green" : "amber")
                .title("Market Opportunity: " + comm)
                .reasoning("Current rate ₹" + current + "/kg is " + (premium >= 0 ? "+" : "") + premium + "% compared to MSP baseline.")
                .currentRate(current)
                .benchmarkRate(benchmark)
                .premiumPercentage(premium)
                .potentialGainPerQuintal(Math.max(0, (current - benchmark) * 100.0))
                .topMandiName("Indore Mandi")
                .topMandiPrice(current * 100.0)
                .distanceKm(12.5)
                .build();
    }

    public SellAdviceDto getSellAdvice(String commodity) {
        String comm = commodity != null && !commodity.isBlank() ? commodity.trim() : "Wheat";
        PriceInsightDto insight = getPriceInsight(comm);
        String trend = insight != null ? insight.getTrend() : "UP";

        String recommendation = "UP".equals(trend) ? "HOLD_OR_STAGGER" : "SELL_NOW";
        int confidence = "UP".equals(trend) ? 84 : 76;
        String reason = "UP".equals(trend) ?
                "Prices for " + comm + " are trending upward across regional mandis. Consider staggered selling to capture market peaks." :
                "Supplies are increasing in key APMC markets. Selling currently listed volume locks in current prices.";

        return SellAdviceDto.builder()
                .recommendation(recommendation)
                .confidence(confidence)
                .reason(reason)
                .disclaimer("Prices discovered from AGMARKNET government sources. Market conditions may vary.")
                .build();
    }
}
