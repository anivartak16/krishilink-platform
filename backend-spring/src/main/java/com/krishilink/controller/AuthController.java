package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.dto.AuthDtos.*;
import com.krishilink.entity.User;
import com.krishilink.security.UserPrincipal;
import com.krishilink.service.AadhaarKycService;
import com.krishilink.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AadhaarKycService aadhaarKycService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest req) {
        AuthResponse res = authService.register(req);
        return ResponseEntity.ok(ApiResponse.ok(res, "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest req) {
        AuthResponse res = authService.login(req);
        return ResponseEntity.ok(ApiResponse.ok(res, "Login successful"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout() {
        return ResponseEntity.ok(ApiResponse.ok(Map.of(), "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> me(@AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.ok(ApiResponse.ok(authService.sanitizeUser(principal.getUser())));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateProfileRequest req) {
        Map<String, Object> updated = authService.updateProfile(principal.getUser(), req);
        return ResponseEntity.ok(ApiResponse.ok(updated, "Profile updated successfully"));
    }

    @PostMapping("/ekyc")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyEkyc(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        Map<String, Object> updated = authService.verifyEkyc(principal.getUser(), body);
        return ResponseEntity.ok(ApiResponse.ok(updated, "e-KYC verified successfully"));
    }

    @GetMapping("/profile/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(authService.getProfile(id), "User profile retrieved"));
    }

    @PostMapping("/kyc/otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendKycOtp(@RequestBody KycOtpRequest req) {
        Map<String, Object> res = aadhaarKycService.generateOtp(req.getAadhaarNumber());
        return ResponseEntity.ok(ApiResponse.ok(res, (String) res.get("message")));
    }

    @PostMapping("/kyc/resend-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resendKycOtp(@RequestBody Map<String, String> body) {
        String clientId = body.get("client_id");
        Map<String, Object> res = aadhaarKycService.resendOtp(clientId);
        return ResponseEntity.ok(ApiResponse.ok(res, (String) res.get("message")));
    }

    @PostMapping("/kyc/verify-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyKycOtp(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody KycVerifyRequest req) {
        User user = principal != null ? principal.getUser() : null;
        Map<String, Object> res = aadhaarKycService.verifyOtp(user, req.getClientId(), req.getOtp());
        return ResponseEntity.ok(ApiResponse.ok(res, (String) res.get("message")));
    }
}
