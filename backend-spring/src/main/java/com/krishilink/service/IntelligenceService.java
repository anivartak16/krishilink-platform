package com.krishilink.service;

import com.krishilink.dto.MatchDto;
import com.krishilink.dto.PriceDtos.*;
import com.krishilink.entity.*;
import com.krishilink.enums.LotStatus;
import com.krishilink.repository.*;
import com.krishilink.util.GeoUtils;
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
    private final LotRepository lotRepository;

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
            return null;
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

        for (MarketPrice p : prices) {
            String mName = p.getMarket() != null ? p.getMarket().getName() : (p.getMarketName() != null ? p.getMarketName() : "Local Mandi");
            String key = mName.toLowerCase().trim();

            if (!newest.containsKey(key)) {
                List<Double> mandiCoords = p.getMarket() != null && p.getMarket().getLongitude() != null ?
                        List.of(p.getMarket().getLongitude(), p.getMarket().getLatitude()) :
                        GeoUtils.resolveCoordinates(p.getDistrict(), p.getState(), mName);

                double dist = GeoUtils.calculateDistanceKm(userCoords, mandiCoords);
                boolean isSameTown = location != null && !location.isBlank() && mName.toLowerCase().contains(location.toLowerCase());
                boolean isSameDistrict = district != null && !district.isBlank() &&
                        (mName.toLowerCase().contains(district.toLowerCase()) ||
                                (p.getDistrict() != null && p.getDistrict().equalsIgnoreCase(district)));

                if (isSameTown) dist = 3.0;
                else if (isSameDistrict && dist <= 10.0) dist = 15.0;

                double modalPricePerQtl = p.getModalPrice() != null ?
                        (p.getModalPrice() < 150 ? p.getModalPrice() * 100 : p.getModalPrice()) : 2400.0;
                double modalPricePerKg = Math.round((modalPricePerQtl / 100.0) * 100.0) / 100.0;

                double transportCostPerQtl = Math.max(5.0, Math.round(dist * 0.8));
                double netPricePerQtl = Math.max(1.0, modalPricePerQtl - transportCostPerQtl);
                double netPricePerKg = Math.round((netPricePerQtl / 100.0) * 100.0) / 100.0;

                Map<String, Object> marketInfo = new HashMap<>();
                marketInfo.put("name", mName);
                marketInfo.put("location", (p.getDistrict() != null ? p.getDistrict() : "Indore") + ", " + (p.getState() != null ? p.getState() : "Madhya Pradesh"));
                marketInfo.put("district", p.getDistrict());
                marketInfo.put("state", p.getState());
                marketInfo.put("geo", Map.of("type", "Point", "coordinates", mandiCoords));

                newest.put(key, MarketRecommendationDto.builder()
                        .id(p.getId())
                        ._id(p.getId().toString())
                        .commodity(p.getCommodity())
                        .variety(p.getVariety() != null ? p.getVariety() : "Standard")
                        .grade(p.getGrade() != null ? p.getGrade() : "FAQ")
                        .arrivalDate(p.getPriceDate())
                        .modalPrice(modalPricePerKg)
                        .modalPricePerQtl(modalPricePerQtl)
                        .minPrice(p.getMinPrice() != null ? p.getMinPrice() : modalPricePerKg - 2)
                        .maxPrice(p.getMaxPrice() != null ? p.getMaxPrice() : modalPricePerKg + 2)
                        .netPrice(netPricePerKg)
                        .netPricePerQtl(netPricePerQtl)
                        .distanceKm(dist)
                        .transportCostPerKm(0.8)
                        .estimatedTransportCost(Math.round((transportCostPerQtl / 100.0) * 100.0) / 100.0)
                        .isSameTown(isSameTown)
                        .isSameDistrict(isSameDistrict)
                        .source("AGMARKNET")
                        .market(marketInfo)
                        .build());
            }
        }

        List<MarketRecommendationDto> list = new ArrayList<>(newest.values());
        if (list.isEmpty()) return list;

        double minNet = list.stream().mapToDouble(MarketRecommendationDto::getNetPrice).min().orElse(1.0);
        double maxNet = list.stream().mapToDouble(MarketRecommendationDto::getNetPrice).max().orElse(1.0);
        double maxDist = list.stream().mapToDouble(MarketRecommendationDto::getDistanceKm).max().orElse(100.0);
        double nearestDist = list.stream().mapToDouble(MarketRecommendationDto::getDistanceKm).min().orElse(10.0);

        for (MarketRecommendationDto item : list) {
            double priceScore = maxNet == minNet ? 35.0 : ((item.getNetPrice() - minNet) / (maxNet - minNet)) * 35.0;
            double distScore = Math.max(0, 35.0 - (item.getDistanceKm() / maxDist) * 35.0);
            double bonus = item.getIsSameTown() ? 25.0 : item.getIsSameDistrict() ? 18.0 : 0.0;
            int score = (int) Math.min(100, Math.round(priceScore + distScore + 8.0 + bonus));
            item.setRecommendationScore(score);

            List<String> reasons = List.of(
                    item.getIsSameTown() ? "📍 In your town" : "Distance: ~" + item.getDistanceKm() + " km",
                    "Modal: ₹" + item.getModalPricePerQtl() + "/qtl (₹" + item.getModalPrice() + "/kg)",
                    "Net return: ~₹" + item.getNetPricePerQtl() + "/qtl after transport"
            );
            item.setRecommendationReasons(reasons);

            String badge = "";
            if (item.getIsSameTown()) badge = "📍 IN YOUR TOWN";
            else if (item.getIsSameDistrict()) badge = "📍 LOCAL DISTRICT APMC";
            else if (item.getDistanceKm().equals(nearestDist)) badge = "⚡ NEAREST APMC";
            else if (item.getNetPrice().equals(maxNet)) badge = "💰 TOP RETURN";
            item.setBadge(badge);
            item.setIsNearest(item.getDistanceKm().equals(nearestDist));
        }

        list.sort((a, b) -> Integer.compare(b.getRecommendationScore(), a.getRecommendationScore()));
        if (!list.isEmpty()) list.get(0).setIsRecommended(true);

        return list.stream().limit(10).toList();
    }

    public SellAdviceDto getSellAdvice(String commodity) {
        PriceInsightDto insight = getPriceInsight(commodity);
        if (insight == null) {
            return SellAdviceDto.builder()
                    .recommendation("COMPARE_MARKETS")
                    .confidence(70)
                    .reason("Compare verified nearby mandi prices before committing your produce.")
                    .disclaimer("Rule-based market insight — not financial advice.")
                    .build();
        }

        String rec = "COMPARE_MARKETS";
        String reason = "Compare verified nearby mandi prices before committing produce.";

        if ("UP".equalsIgnoreCase(insight.getTrend()) && insight.getCurrentPrice() < insight.getAverage30Days()) {
            rec = "WAIT";
            reason = "Current price is below the 30-day average and the recent trend is upward.";
        } else if (insight.getCurrentPrice() >= insight.getAverage30Days()) {
            rec = "SELL_NOW";
            reason = "Price is at or above the recent average and active buyer demand is available.";
        }

        return SellAdviceDto.builder()
                .recommendation(rec)
                .confidence(72)
                .reason(reason)
                .disclaimer("Rule-based market insight — not financial advice or guaranteed price prediction.")
                .build();
    }

    public PriceOpportunityAlertDto getPriceOpportunityAlert(String commodity) {
        String comm = commodity != null && !commodity.isBlank() ? commodity.trim() : "Wheat";
        PriceInsightDto insight = getPriceInsight(comm);
        double mspKg = MSP_BENCHMARKS.getOrDefault(comm.toLowerCase(), 22.75);
        double currentRate = insight != null ? insight.getCurrentPrice() : mspKg + 2.0;
        double avg30 = insight != null ? insight.getAverage30Days() : mspKg;
        double changePct = avg30 > 0 ? Math.round(((currentRate - avg30) / avg30) * 100.0) : 0.0;

        String rec = "MONITOR";
        String badgeColor = "blue";
        String title = "Market Price in Equilibrium";
        String reasoning = "Current rate is ₹" + currentRate + "/kg against ₹" + avg30 + "/kg benchmark.";

        if (changePct >= 5.0 || currentRate > mspKg * 1.15) {
            rec = "SELL_NOW";
            badgeColor = "green";
            title = "🟢 SELL OPPORTUNITY: " + comm + " at Seasonal Peak (+" + changePct + "%)";
            reasoning = "Mandi prices have surged above baseline and official MSP. High buyer liquidity present.";
        } else if (changePct <= -4.0 || currentRate < avg30) {
            rec = "HOLD_IN_STORAGE";
            badgeColor = "amber";
            title = "🟡 HOLD RECOMMENDED: " + comm + " Under Market Dip (" + changePct + "%)";
            reasoning = "Current prices are below 30-day averages. Holding in warehouse storage is advised.";
        }

        return PriceOpportunityAlertDto.builder()
                .commodity(comm)
                .recommendation(rec)
                .badgeColor(badgeColor)
                .title(title)
                .reasoning(reasoning)
                .currentRate(currentRate)
                .benchmarkRate(avg30)
                .changePercentage(changePct)
                .mspKg(mspKg)
                .mspQtl(mspKg * 100.0)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public List<MatchDto> getMatchesFor(Demand demand, User currentUser) {
        if (demand.getDeadline() != null && demand.getDeadline().isBefore(LocalDate.now())) {
            return List.of();
        }

        List<Lot> lots = lotRepository.findByCommodityIgnoreCaseAndStatusIn(
                demand.getCommodity(),
                List.of(LotStatus.AVAILABLE, LotStatus.PARTIALLY_SOLD)
        );

        List<MatchDto> matches = new ArrayList<>();

        for (Lot l : lots) {
            if (l.getAvailableUntil() != null && l.getAvailableUntil().isBefore(LocalDate.now())) {
                continue;
            }

            // 1. Quantity fit (25 pts)
            double reqQty = demand.getRequiredQuantity() != null && demand.getRequiredQuantity() > 0 ?
                    demand.getRequiredQuantity() : 1.0;
            double lotQty = l.getRemainingQuantity() != null ? l.getRemainingQuantity() : 0.0;
            double qtyRatio = Math.min(lotQty / reqQty, 1.0);
            int quantityScore = (int) Math.round(qtyRatio * 25.0);

            // 2. Quality grade fit (25 pts)
            String lotGrade = l.getQuality() != null && l.getQuality().getGrade() != null ?
                    l.getQuality().getGrade().toLowerCase() : "grade b";
            String demGrade = demand.getRequiredQuality() != null ? demand.getRequiredQuality().toLowerCase() : "grade a";
            int qualityScore = 15;
            if (lotGrade.equals(demGrade) || lotGrade.contains(demGrade) || demGrade.contains(lotGrade)) {
                qualityScore = 25;
            } else if (lotGrade.contains("a") && demGrade.contains("b")) {
                qualityScore = 25;
            }

            // 3. Price fit (25 pts)
            int priceScore;
            if (l.getExpectedPrice() <= demand.getMaxPrice()) {
                priceScore = 25;
            } else {
                double diff = l.getExpectedPrice() - demand.getMaxPrice();
                priceScore = (int) Math.max(0, Math.round(25.0 - (diff / Math.max(demand.getMaxPrice(), 1.0)) * 30.0));
            }

            // 4. Location fit (15 pts)
            String lotLoc = l.getLocation() != null ? l.getLocation().toLowerCase().trim() : "";
            String demLoc = demand.getPreferredLocation() != null ? demand.getPreferredLocation().toLowerCase().trim() : "";
            int locationScore = 8;
            if (demLoc.isBlank()) {
                locationScore = 12;
            } else if (lotLoc.equals(demLoc) || lotLoc.contains(demLoc) || demLoc.contains(lotLoc)) {
                locationScore = 15;
            }

            // 5. Freshness & availability (10 pts)
            int freshnessScore = l.getAvailableUntil() != null ? 10 : 7;

            // 6. Organic Bonus
            int organicBonus = 0;
            String demFarming = demand.getFarmingType() != null ? demand.getFarmingType().toLowerCase() : "any";
            String lotFarming = l.getFarmingType() != null ? l.getFarmingType().toLowerCase() : "conventional";
            String lotOrganicStatus = l.getOrganicCertificationStatus() != null ? l.getOrganicCertificationStatus() : "not_verified";

            if ("organic".equals(demFarming)) {
                if ("organic".equals(lotFarming) && "verified".equals(lotOrganicStatus)) {
                    organicBonus = 15;
                } else if ("organic".equals(lotFarming)) {
                    organicBonus = 5;
                } else {
                    organicBonus = -30;
                }
            } else if ("organic".equals(lotFarming) && "verified".equals(lotOrganicStatus)) {
                organicBonus = 5;
            }

            int matchScore = (int) Math.max(0, Math.min(100,
                    quantityScore + qualityScore + priceScore + locationScore + freshnessScore + organicBonus));

            boolean isUserLot = currentUser != null && l.getOwner().getId().equals(currentUser.getId());
            boolean isFpoLot = "FPO".equalsIgnoreCase(l.getOwnerType());

            double estimatedTransport = 2.0;
            double mandiFee = Math.round(l.getExpectedPrice() * 0.015 * 100.0) / 100.0;
            double netRealisation = Math.max(1.0, Math.round((l.getExpectedPrice() - estimatedTransport - mandiFee) * 100.0) / 100.0);

            List<String> reasons = List.of(
                    quantityScore + "/25 quantity match (" + lotQty + " kg available)",
                    qualityScore + "/25 quality grade compatibility (" + lotGrade.toUpperCase() + " vs " + demGrade.toUpperCase() + ")",
                    priceScore + "/25 price budget fit (₹" + l.getExpectedPrice() + "/kg vs budget ₹" + demand.getMaxPrice() + "/kg)",
                    locationScore + "/15 location proximity (" + (lotLoc.isBlank() ? "Local" : lotLoc) + " -> " + (demLoc.isBlank() ? "Destination" : demLoc) + ")",
                    freshnessScore + "/10 batch freshness"
            );

            matches.add(MatchDto.builder()
                    .lot(l)
                    .matchScore(matchScore)
                    .isUserLot(isUserLot)
                    .isFpoLot(isFpoLot)
                    .netRealisation(netRealisation)
                    .reasons(reasons)
                    .breakdown(Map.of(
                            "quantity", quantityScore,
                            "quality", qualityScore,
                            "price", priceScore,
                            "location", locationScore,
                            "availability", freshnessScore
                    ))
                    .build());
        }

        matches.sort((a, b) -> Integer.compare(b.getMatchScore(), a.getMatchScore()));
        return matches;
    }
}
