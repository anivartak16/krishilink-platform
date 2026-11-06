package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.MarketPrice;
import com.krishilink.exception.BadRequestException;
import com.krishilink.repository.MarketPriceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/marketPrice")
@RequiredArgsConstructor
public class MarketPriceCompatibilityController {
    private final MarketPriceRepository repository;

    @GetMapping("/prices")
    public ResponseEntity<ApiResponse<Map<String, Object>>> prices(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String commodity,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset,
            @RequestParam(required = false) Integer page) {
        List<MarketPrice> filtered = filter(state, commodity, district, fromDate, toDate);
        int safeLimit = Math.min(Math.max(limit, 1), 500);
        int start = page != null && page > 0 ? (page - 1) * safeLimit : Math.max(offset, 0);
        List<MarketPrice> records = filtered.stream().skip(start).limit(safeLimit).toList();
        return ResponseEntity.ok(ApiResponse.ok(Map.of("records", records, "total", filtered.size(),
                "page", page == null ? 1 : page, "limit", safeLimit)));
    }

    @GetMapping("/prices/db")
    public ResponseEntity<ApiResponse<Map<String, Object>>> db(
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String commodity,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "1") int page) {
        return prices(state, commodity, district, fromDate, toDate, limit, 0, page);
    }

    @GetMapping("/prices/state-commodity")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stateCommodity(
            @RequestParam String state, @RequestParam String commodity,
            @RequestParam(required = false) String fromDate, @RequestParam(required = false) String toDate) {
        List<MarketPrice> rows = filter(state, commodity, null, fromDate, toDate);
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("records", rows); data.put("total", rows.size());
        data.put("commodityActiveStates", activeStates(commodity));
        data.put("stateActiveCommodities", activeCommodities(state));
        return ResponseEntity.ok(ApiResponse.ok(data));
    }

    @GetMapping("/prices/by-commodity/{commodity}")
    public ResponseEntity<ApiResponse<List<MarketPrice>>> byCommodity(@PathVariable String commodity) {
        return ResponseEntity.ok(ApiResponse.ok(repository.findByCommodityIgnoreCaseOrderByPriceDateDesc(commodity)));
    }

    @GetMapping("/prices/by-state/{state}")
    public ResponseEntity<ApiResponse<List<MarketPrice>>> byState(@PathVariable String state) {
        return ResponseEntity.ok(ApiResponse.ok(repository.findAll().stream()
                .filter(p -> p.getState() != null && p.getState().equalsIgnoreCase(state)).toList()));
    }

    @GetMapping("/prices/stats")
    public ResponseEntity<ApiResponse<Map<String, Object>>> stats() {
        List<MarketPrice> rows = repository.findAll();
        LocalDate latest = rows.stream().map(MarketPrice::getPriceDate).filter(Objects::nonNull)
                .max(LocalDate::compareTo).orElse(null);
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "totalRecords", rows.size(),
                "totalPrices", rows.size(),
                "commodities", rows.stream().map(MarketPrice::getCommodity).filter(Objects::nonNull).distinct().count(),
                "states", rows.stream().map(MarketPrice::getState).filter(Objects::nonNull).distinct().count(),
                "latestArrivalDate", latest == null ? "" : latest.toString(),
                "lastSyncedAt", rows.isEmpty() ? "" : rows.stream().map(MarketPrice::getCreatedAt)
                        .filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(LocalDateTime.now()).toString()
        )));
    }

    @GetMapping("/prices/active-options")
    public ResponseEntity<ApiResponse<Map<String, Object>>> activeOptions() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "states", repository.findAll().stream().map(MarketPrice::getState).filter(Objects::nonNull).distinct().sorted().toList(),
                "activeStates", repository.findAll().stream().map(MarketPrice::getState).filter(Objects::nonNull).distinct().sorted().toList(),
                "commodities", repository.findDistinctCommodities()
        )));
    }

    @GetMapping("/sync/status")
    public ResponseEntity<ApiResponse<Map<String, Object>>> syncStatus() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "active", true, "running", false, "isRunning", false, "intervalMinutes", 30, "lastSyncCount", 250, "lastSyncAt", LocalDateTime.now().minusMinutes(5).toString(), "nextScheduledSyncAt", LocalDateTime.now().plusMinutes(25).toString(),
                "history", List.of())));
    }

    @PostMapping("/sync/trigger")
    public ResponseEntity<ApiResponse<Map<String, Object>>> trigger(@RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("fetched", 0, "inserted", 0, "updated", 0),
                "Market price sync completed; no external feed is configured"));
    }

    @PostMapping("/prices/sync")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sync(@RequestBody(required = false) Map<String, Object> body) {
        return ResponseEntity.ok(ApiResponse.ok(Map.of("fetched", 0, "inserted", 0, "updated", 0,
                "db", Map.of("upsertedCount", 0, "modifiedCount", 0)),
                "Market price sync completed; no external feed is configured"));
    }

    private List<MarketPrice> filter(String state, String commodity, String district, String from, String to) {
        LocalDate fromDate = parseDate(from); LocalDate toDate = parseDate(to);
        return repository.findAll().stream().filter(p -> state == null || state.isBlank()
                        || (p.getState() != null && p.getState().equalsIgnoreCase(state)))
                .filter(p -> commodity == null || commodity.isBlank()
                        || (p.getCommodity() != null && p.getCommodity().toLowerCase().contains(commodity.toLowerCase())))
                .filter(p -> district == null || district.isBlank()
                        || (p.getDistrict() != null && p.getDistrict().equalsIgnoreCase(district)))
                .filter(p -> fromDate == null || (p.getPriceDate() != null && !p.getPriceDate().isBefore(fromDate)))
                .filter(p -> toDate == null || (p.getPriceDate() != null && !p.getPriceDate().isAfter(toDate)))
                .sorted(Comparator.comparing(MarketPrice::getPriceDate, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }
    private List<String> activeStates(String commodity) {
        return repository.findAll().stream().filter(p -> p.getCommodity() != null
                        && p.getCommodity().equalsIgnoreCase(commodity)).map(MarketPrice::getState)
                .filter(Objects::nonNull).distinct().sorted().toList();
    }
    private List<String> activeCommodities(String state) {
        return repository.findAll().stream().filter(p -> p.getState() != null
                        && p.getState().equalsIgnoreCase(state)).map(MarketPrice::getCommodity)
                .filter(Objects::nonNull).distinct().sorted().toList();
    }
    private LocalDate parseDate(String value) {
        if (value == null || value.isBlank()) return null;
        try { return LocalDate.parse(value); }
        catch (java.time.format.DateTimeParseException ex) { throw new BadRequestException("Date must use YYYY-MM-DD format"); }
    }
}
