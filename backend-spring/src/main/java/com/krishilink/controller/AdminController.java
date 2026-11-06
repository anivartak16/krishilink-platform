package com.krishilink.controller;

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
    private final TransactionRepository transactionRepository;
    private final AuthService authService;

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getUsers() {
        List<Map<String, Object>> list = userRepository.findAll().stream()
                .map(authService::sanitizeUser)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PatchMapping("/users/{id}/verify")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyUser(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
        user.setVerification(VerificationStatus.VERIFIED);
        user.setVerificationBadge("ADMIN_VERIFIED");
        user = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok(authService.sanitizeUser(user), "User verified"));
    }

    @GetMapping({"/stats", "/summary"})
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminStats() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(
                "totalUsers", userRepository.count(),
                "totalLots", lotRepository.count(),
                "totalTransactions", transactionRepository.count()
        )));
    }
}
