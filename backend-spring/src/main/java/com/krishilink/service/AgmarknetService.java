package com.krishilink.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishilink.entity.Market;
import com.krishilink.entity.MarketPrice;
import com.krishilink.repository.MarketPriceRepository;
import com.krishilink.repository.MarketRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AgmarknetService {

    private static final DateTimeFormatter API_DATE = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final String BASE_URL = "https://api.data.gov.in/resource";

    private final RestClient.Builder restClientBuilder;
    private final ObjectMapper objectMapper;
    private final MarketRepository marketRepository;
    private final MarketPriceRepository marketPriceRepository;

    @Value("${agmarknet.api-key:}")
    private String apiKey;

    @Value("${agmarknet.resource-id:9ef84268-d588-465a-a308-a864a43d0070}")
    private String resourceId;

    @Transactional
    public FetchResult fetchPrices(String state, String commodity, String district,
                                   String market, LocalDate arrivalDate, int limit) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("AGMARKNET_API_KEY is not configured.");
        }

        int safeLimit = Math.max(1, Math.min(limit, 1000));
        UriComponentsBuilder uri = UriComponentsBuilder
                .fromUriString(BASE_URL + "/" + resourceId.trim())
                .queryParam("api-key", apiKey.trim())
                .queryParam("format", "json")
                .queryParam("limit", safeLimit);
        addFilter(uri, "state", state);
        addFilter(uri, "commodity", commodity);
        addFilter(uri, "district", district);
        addFilter(uri, "market", market);
        if (arrivalDate != null) {
            uri.queryParam("filters[arrival_date]", API_DATE.format(arrivalDate));
        }

        try {
            String body = restClientBuilder.build().get()
                    .uri(uri.build(true).toUri())
                    .retrieve()
                    .onStatus(HttpStatusCode::isError, (request, response) -> {
                        throw new IllegalStateException("AGMARKNET returned HTTP " + response.getStatusCode().value());
                    })
                    .body(String.class);
            return persistResponse(objectMapper.readTree(body));
        } catch (RestClientException | com.fasterxml.jackson.core.JsonProcessingException ex) {
            throw new IllegalStateException("Unable to fetch prices from AGMARKNET.", ex);
        }
    }

    private void addFilter(UriComponentsBuilder uri, String name, String value) {
        if (value != null && !value.isBlank()) {
            uri.queryParam("filters[" + name + "]", value.trim());
        }
    }

    private FetchResult persistResponse(JsonNode root) {
        List<MarketPrice> prices = new ArrayList<>();
        JsonNode rows = root.path("records");
        for (JsonNode row : rows) {
            String marketName = text(row, "market");
            if (marketName.isBlank()) continue;

            Market market = marketRepository.findByNameIgnoreCase(marketName)
                    .orElseGet(() -> marketRepository.save(Market.builder()
                            .name(marketName)
                            .district(text(row, "district"))
                            .state(text(row, "state"))
                            .location(text(row, "district") + ", " + text(row, "state"))
                            .commodities(text(row, "commodity"))
                            .transportCostPerKm(8.0)
                            .reviewAverage(4.2)
                            .reviewCount(0)
                            .build()));

            MarketPrice price = MarketPrice.builder()
                    .market(market)
                    .marketName(marketName)
                    .state(text(row, "state"))
                    .district(text(row, "district"))
                    .commodity(text(row, "commodity"))
                    .variety(text(row, "variety"))
                    .grade(text(row, "grade"))
                    .priceDate(parseDate(text(row, "arrival_date")))
                    .minPrice(number(row, "min_price"))
                    .maxPrice(number(row, "max_price"))
                    .modalPrice(number(row, "modal_price"))
                    .arrivalVolume(number(row, "arrival"))
                    .source("AGMARKNET")
                    .unit("KG")
                    .build();
            prices.add(price);
        }

        if (!prices.isEmpty()) {
            marketPriceRepository.saveAll(prices);
        }
        int total = root.path("total").asInt(prices.size());
        return new FetchResult(total, prices.size(), prices);
    }

    private String text(JsonNode node, String field) {
        return node.path(field).asText("").trim();
    }

    private Double number(JsonNode node, String field) {
        String value = text(node, field).replace(",", "");
        try {
            return value.isBlank() ? 0.0 : Double.valueOf(value);
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private LocalDate parseDate(String value) {
        if (value.isBlank()) return null;
        try {
            return LocalDate.parse(value, API_DATE);
        } catch (DateTimeParseException ex) {
            return null;
        }
    }

    public record FetchResult(int total, int count, List<MarketPrice> records) {
    }
}
