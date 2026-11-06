package com.krishilink.userservice.controller;

import com.krishilink.userservice.dto.ApiResponse;
import com.krishilink.userservice.entity.User;
import com.krishilink.userservice.enums.VerificationStatus;
import com.krishilink.userservice.repository.UserRepository;
import com.krishilink.userservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserInternalController {

    private final UserRepository userRepository;
    private final AuthService authService;

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getUserById(@PathVariable Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        return ResponseEntity.ok(ApiResponse.ok(authService.sanitizeUser(user)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getAllUsers() {
        List<Map<String, Object>> list = userRepository.findAll().stream()
                .map(authService::sanitizeUser)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @PatchMapping("/{id}/verification")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateUserVerification(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with id: " + id));
        String target = body.getOrDefault("verification", "VERIFIED");
        if ("PENDING".equalsIgnoreCase(target)) {
            user.setVerification(VerificationStatus.PENDING);
            user.setVerificationBadge(null);
            user.setKycVerified(false);
        } else {
            user.setVerification(VerificationStatus.VERIFIED);
            user.setVerificationBadge("ADMIN_VERIFIED");
            user.setKycVerified(true);
        }
        user = userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.ok(authService.sanitizeUser(user)));
    }
}
