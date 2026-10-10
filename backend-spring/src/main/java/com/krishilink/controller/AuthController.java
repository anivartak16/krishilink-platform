package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import com.krishilink.dto.AuthDtos.*;
import com.krishilink.entity.User;
import com.krishilink.security.UserPrincipal;
import com.krishilink.service.AadhaarKycService;
import com.krishilink.service.AuthService;
import com.krishilink.enums.Role;
import com.krishilink.enums.VerificationStatus;
import com.krishilink.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.LinkedHashMap;

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

    @GetMapping("/profile/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(authService.getProfile(id), "User profile retrieved"));
    }

    @PostMapping("/kyc/otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendKycOtp(@RequestBody KycOtpRequest req) {
        Map<String, Object> res = aadhaarKycService.generateOtp(req.getAadhaarNumber());
        return ResponseEntity.ok(ApiResponse.ok(res, (String) res.get("message")));
    }

    @PostMapping("/kyc/send-otp")
    public ResponseEntity<ApiResponse<Map<String, Object>>> sendKycOtpAlias(@RequestBody KycOtpRequest req) {
        return sendKycOtp(req);
    }

    @PostMapping("/ekyc")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyEkyc(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody Map<String, Object> body) {
        if (principal == null) throw new BadRequestException("Authentication required");
        String type = body.get("ekycType") == null ? "AADHAAR" : body.get("ekycType").toString().trim().toUpperCase();
        String idNumber = body.get("idNumber") == null ? "" : body.get("idNumber").toString().replaceAll("\\s+", "");
        if (idNumber.isBlank()) throw new BadRequestException("Identity number is required");
        User user = principal.getUser();
        user.setEkycType(type);
        user.setEkycStatus("VERIFIED");
        user.setEkycVerifiedAt(java.time.LocalDateTime.now());
        user.setKycVerified(true);
        user.setKycVerifiedAt(java.time.LocalDateTime.now());
        user.setVerification(VerificationStatus.VERIFIED);
        user.setVerificationBadge(user.getRole() == Role.BUYER ? "VERIFIED_BUYER" : "EKYC_VERIFIED_FARMER");
        if (idNumber.length() >= 4) user.setAadhaarLast4(idNumber.substring(idNumber.length() - 4));
        user = authService.saveVerifiedUser(user);
        return ResponseEntity.ok(ApiResponse.ok(authService.sanitizeUser(user), "e-KYC verified successfully"));
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
        if (user != null) {
            res = new LinkedHashMap<>(res);
            res.put("user", authService.sanitizeUser(user));
        }
        return ResponseEntity.ok(ApiResponse.ok(res, (String) res.get("message")));
    }
}
