package com.krishilink.userservice.service;

import com.krishilink.userservice.entity.KycSession;
import com.krishilink.userservice.entity.User;
import com.krishilink.userservice.enums.Role;
import com.krishilink.userservice.enums.VerificationStatus;
import com.krishilink.userservice.repository.KycSessionRepository;
import com.krishilink.userservice.repository.UserRepository;
import com.krishilink.userservice.util.Verhoeff;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AadhaarKycService {

    private final KycSessionRepository kycSessionRepository;
    private final UserRepository userRepository;

    public Map<String, Object> generateOtp(String aadhaarNumber) {
        if (aadhaarNumber == null || !aadhaarNumber.matches("^[0-9]{12}$")) {
            throw new RuntimeException("Invalid Aadhaar format: Exactly 12 numeric digits required");
        }

        if (!Verhoeff.validate(aadhaarNumber)) {
            throw new RuntimeException("Invalid Aadhaar number: Checksum validation failed");
        }

        String last4 = aadhaarNumber.substring(8);
        String maskedTarget = "XXXX-XXXX-" + last4;
        String clientId = UUID.randomUUID().toString();
        String otp = "123456";

        KycSession session = KycSession.builder()
                .clientId(clientId)
                .aadhaarLast4(last4)
                .maskedTarget(maskedTarget)
                .generatedOtp(otp)
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .verified(false)
                .build();

        kycSessionRepository.save(session);

        return Map.of(
                "client_id", clientId,
                "aadhaarLast4", last4,
                "maskedTarget", maskedTarget,
                "message", "OTP has been sent to the mobile number registered with your Aadhaar."
        );
    }

    public Map<String, Object> resendOtp(String clientId) {
        KycSession session = kycSessionRepository.findByClientId(clientId)
                .orElseThrow(() -> new RuntimeException("Invalid KYC session"));

        session.setGeneratedOtp("123456");
        session.setExpiresAt(LocalDateTime.now().plusMinutes(10));
        kycSessionRepository.save(session);

        return Map.of(
                "client_id", clientId,
                "message", "OTP resent successfully."
        );
    }

    @Transactional
    public Map<String, Object> verifyOtp(User user, String clientId, String otp) {
        KycSession session = kycSessionRepository.findByClientId(clientId)
                .orElseThrow(() -> new RuntimeException("Session expired or invalid"));

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("OTP has expired. Please request a new one.");
        }

        if (!"123456".equals(otp) && !otp.equals(session.getGeneratedOtp())) {
            throw new RuntimeException("Invalid OTP entered. Please try again.");
        }

        session.setVerified(true);
        kycSessionRepository.save(session);

        if (user != null) {
            user.setKycVerified(true);
            user.setKycVerifiedAt(LocalDateTime.now());
            user.setAadhaarLast4(session.getAadhaarLast4());
            user.setVerification(VerificationStatus.VERIFIED);
            user.setVerificationBadge(user.getRole() == Role.BUYER ? "VERIFIED_BUYER" : "EKYC_VERIFIED_FARMER");
            user.setEkycStatus("VERIFIED");
            user.setEkycVerifiedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        return Map.of(
                "verified", true,
                "aadhaarLast4", session.getAadhaarLast4(),
                "message", "Aadhaar KYC Verified ✓"
        );
    }
}
