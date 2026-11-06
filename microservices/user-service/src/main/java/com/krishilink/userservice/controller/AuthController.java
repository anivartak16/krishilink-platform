package com.krishilink.userservice.controller;

import com.krishilink.userservice.dto.ApiResponse;
import com.krishilink.userservice.dto.AuthDtos.*;
import com.krishilink.userservice.entity.User;
import com.krishilink.userservice.repository.UserRepository;
import com.krishilink.userservice.security.UserPrincipal;
import com.krishilink.userservice.service.AadhaarKycService;
import com.krishilink.userservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AadhaarKycService aadhaarKycService;
    private final UserRepository userRepository;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@RequestBody RegisterRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.register(req), "Registration successful"));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@RequestBody LoginRequest req) {
        return ResponseEntity.ok(ApiResponse.ok(authService.login(req), "Login successful"));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout() {
        return ResponseEntity.ok(ApiResponse.ok(null, "Logged out successfully"));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getCurrentUser(@AuthenticationPrincipal UserPrincipal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body(ApiResponse.fail("Not authenticated", "UNAUTHORIZED"));
        }
        return ResponseEntity.ok(ApiResponse.ok(authService.sanitizeUser(principal.getUser())));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> updateProfile(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody UpdateProfileRequest req) {
        User updated = authService.updateProfile(principal.getUser(), req);
        return ResponseEntity.ok(ApiResponse.ok(authService.sanitizeUser(updated), "Profile updated successfully"));
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

    @PostMapping({"/kyc/otp", "/kyc/send-otp"})
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
        Map<String, Object> mutableRes = new LinkedHashMap<>(res);
        if (user != null) {
            User reloaded = userRepository.findById(user.getId()).orElse(user);
            mutableRes.put("user", authService.sanitizeUser(reloaded));
        }
        return ResponseEntity.ok(ApiResponse.ok(mutableRes, (String) mutableRes.get("message")));
    }
}
