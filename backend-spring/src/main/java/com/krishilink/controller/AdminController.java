package com.krishilink.controller;

import java.math.BigDecimal;

import com.krishilink.dto.ApiResponse;
import com.krishilink.entity.User;
import com.krishilink.enums.VerificationStatus;
import com.krishilink.exception.ResourceNotFoundException;
import com.krishilink.repository.LotRepository;
import com.krishilink.repository.TransactionRepository;
import com.krishilink.repository.UserRepository;
import com.krishilink.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final UserRepository userRepository;
    private final LotRepository lotRepository;
    private final com.krishilink.repository.OfferRepository offerRepository;
    private final TransactionRepository transactionRepository;
    private final com.krishilink.repository.PaymentRepository paymentRepository;
    private final AuthService authService;
    private final com.krishilink.service.IntelligenceService intelligenceService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUsers() {
        List<Map<String, Object>> list = userRepository.findAll().stream()
                .map(authService::sanitizeUser)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PatchMapping({"/users/{id}", "/users/{id}/verify"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyUser(
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        String targetVerification = "VERIFIED";
        if (body != null && body.containsKey("verification")) {
            targetVerification = String.valueOf(body.get("verification")).toUpperCase();
        }
        if ("PENDING".equalsIgnoreCase(targetVerification)) {
            user.setVerification(VerificationStatus.PENDING);
            user.setVerificationBadge(null);
            user.setKycVerified(false);
        } else {
            user.setVerification(VerificationStatus.VERIFIED);
            user.setVerificationBadge("ADMIN_VERIFIED");
            user.setKycVerified(true);
        }
        user = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok(authService.sanitizeUser(user), "User verification updated"));
    }

    @GetMapping({"/stats", "/summary"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminStats() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "totalUsers", userRepository.count(),
                "totalLots", lotRepository.count(),
                "totalTransactions", transactionRepository.count()
        )));
    }

    @GetMapping("/analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAnalytics() {
        // Users by role
        Map<String, Long> userCounts = userRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(u -> u.getRole().name(), java.util.stream.Collectors.counting()));
        List<Map<String, Object>> usersList = userCounts.entrySet().stream()
                .map(e -> Map.<String, Object>of("_id", e.getKey(), "count", e.getValue()))
                .toList();

        // Lots by status
        Map<String, Long> lotCounts = lotRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(l -> l.getStatus().name(), java.util.stream.Collectors.counting()));
        List<Map<String, Object>> lotsList = lotCounts.entrySet().stream()
                .map(e -> Map.<String, Object>of("_id", e.getKey(), "count", e.getValue()))
                .toList();

        // Offers by status
        Map<String, Long> offerCounts = offerRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(o -> o.getStatus().name(), java.util.stream.Collectors.counting()));
        List<Map<String, Object>> offersList = offerCounts.entrySet().stream()
                .map(e -> Map.<String, Object>of("_id", e.getKey(), "count", e.getValue()))
                .toList();

        // Transactions by status with values
        Map<String, List<com.krishilink.entity.Transaction>> txByStatus = transactionRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(t -> t.getStatus().name()));
        List<Map<String, Object>> txList = txByStatus.entrySet().stream()
                .map(e -> {
                    BigDecimal sum = e.getValue().stream().map(t -> t.getAmount() != null ? t.getAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return Map.<String, Object>of("_id", e.getKey(), "count", (long) e.getValue().size(), "value", sum);
                })
                .toList();

        // Payments by status with values
        Map<String, List<com.krishilink.entity.Payment>> payByStatus = paymentRepository.findAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(p -> p.getStatus().name()));
        List<Map<String, Object>> payList = payByStatus.entrySet().stream()
                .map(e -> {
                    BigDecimal sum = e.getValue().stream().map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO).reduce(BigDecimal.ZERO, BigDecimal::add);
                    return Map.<String, Object>of("_id", e.getKey(), "count", (long) e.getValue().size(), "value", sum);
                })
                .toList();

        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "users", usersList,
                "lots", lotsList,
                "offers", offersList,
                "transactions", txList,
                "payments", payList
        )));
    }

    @GetMapping("/msp")
    public ResponseEntity<ApiResponse<List<com.krishilink.entity.MspBenchmark>>> getAdminMsp() {
        return ResponseEntity.ok(ApiResponse.ok(intelligenceService.getAllMspBenchmarks()));
    }

    @PutMapping({"/msp/{commodity}", "/msp"})
    public ResponseEntity<ApiResponse<com.krishilink.entity.MspBenchmark>> updateMsp(
            @PathVariable(required = false) String commodity,
            @RequestBody Map<String, Object> body) {
        String comm = commodity != null && !commodity.isBlank() ? commodity :
                (body.get("commodity") != null ? body.get("commodity").toString() : "Wheat");
        Double mspPerQuintal = body.get("mspPerQuintal") != null && !body.get("mspPerQuintal").toString().isBlank() ?
                Double.valueOf(body.get("mspPerQuintal").toString()) : null;
        Double mspPerKg = body.get("mspPerKg") != null && !body.get("mspPerKg").toString().isBlank() ?
                Double.valueOf(body.get("mspPerKg").toString()) : null;
        String season = body.get("marketingSeason") != null ? body.get("marketingSeason").toString() : null;
        Boolean isStatutory = body.get("isStatutoryMsp") != null ? Boolean.valueOf(body.get("isStatutoryMsp").toString()) : null;

        com.krishilink.entity.MspBenchmark updated = intelligenceService.saveOrUpdateMsp(comm, mspPerQuintal, mspPerKg, season, isStatutory);
        return ResponseEntity.ok(ApiResponse.ok(updated, "MSP Benchmark for " + comm + " updated successfully"));
    }
}
