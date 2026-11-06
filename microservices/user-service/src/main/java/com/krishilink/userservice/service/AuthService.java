package com.krishilink.userservice.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishilink.userservice.dto.AuthDtos.*;
import com.krishilink.userservice.entity.User;
import com.krishilink.userservice.enums.Role;
import com.krishilink.userservice.enums.VerificationStatus;
import com.krishilink.userservice.repository.UserRepository;
import com.krishilink.userservice.security.JwtUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (userRepository.existsByEmailIgnoreCase(req.getEmail())) {
            throw new RuntimeException("Email is already registered");
        }

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail().toLowerCase().trim())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole() != null ? req.getRole() : Role.FARMER)
                .phone(req.getPhone())
                .location(req.getLocation())
                .address(req.getAddress())
                .district(req.getDistrict())
                .state(req.getState())
                .pincode(req.getPincode())
                .latitude(req.getLatitude() != null ? req.getLatitude() : 22.7196)
                .longitude(req.getLongitude() != null ? req.getLongitude() : 75.8577)
                .farmName(req.getFarmName())
                .landSize(req.getLandSize())
                .primaryCrop(req.getPrimaryCrop())
                .availableQuantity(req.getAvailableQuantity())
                .organizationName(req.getOrganizationName())
                .buyerType(req.getBuyerType())
                .preferredCommodities(req.getPreferredCommodities())
                .requiredQuantity(req.getRequiredQuantity())
                .qualityRequirements(req.getQualityRequirements())
                .fpoAssociation(req.getFpoAssociation())
                .memberCount(req.getMemberCount())
                .panNumber(req.getPanNumber())
                .gstNumber(req.getGstNumber())
                .registrationNumber(req.getRegistrationNumber())
                .mandiLicenseNumber(req.getMandiLicenseNumber())
                .verification(VerificationStatus.PENDING)
                .active(true)
                .build();

        if (req.getCrops() != null) {
            user.setCrops(req.getCrops().toString());
        }
        if (req.getRequiredCrops() != null) {
            user.setRequiredCrops(req.getRequiredCrops().toString());
        }
        if (req.getQualitySpecs() != null) {
            try {
                user.setQualitySpecsJson(objectMapper.writeValueAsString(req.getQualitySpecs()));
            } catch (Exception ignored) {}
        }

        user = userRepository.save(user);

        String token = jwtUtils.generateToken(user.getEmail(), user.getId(), user.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .user(sanitizeUser(user))
                .build();
    }

    public AuthResponse login(LoginRequest req) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getEmail().toLowerCase().trim(), req.getPassword())
        );
        SecurityContextHolder.getContext().setAuthentication(authentication);

        User user = userRepository.findByEmailIgnoreCase(req.getEmail().trim())
                .orElseThrow(() -> new RuntimeException("User not found"));

        String token = jwtUtils.generateToken(user.getEmail(), user.getId(), user.getRole().name());
        return AuthResponse.builder()
                .token(token)
                .user(sanitizeUser(user))
                .build();
    }

    @Transactional
    public User updateProfile(User user, UpdateProfileRequest req) {
        if (req.getName() != null) user.setName(req.getName());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getLocation() != null) user.setLocation(req.getLocation());
        if (req.getAddress() != null) user.setAddress(req.getAddress());
        if (req.getDistrict() != null) user.setDistrict(req.getDistrict());
        if (req.getState() != null) user.setState(req.getState());
        if (req.getPincode() != null) user.setPincode(req.getPincode());
        if (req.getFarmName() != null) user.setFarmName(req.getFarmName());
        if (req.getLandSize() != null) user.setLandSize(req.getLandSize());
        if (req.getPrimaryCrop() != null) user.setPrimaryCrop(req.getPrimaryCrop());
        if (req.getOrganizationName() != null) user.setOrganizationName(req.getOrganizationName());
        if (req.getPanNumber() != null) user.setPanNumber(req.getPanNumber());
        if (req.getGstNumber() != null) user.setGstNumber(req.getGstNumber());

        if (req.getQualitySpecs() != null) {
            try {
                user.setQualitySpecsJson(objectMapper.writeValueAsString(req.getQualitySpecs()));
            } catch (Exception ignored) {}
        }

        return userRepository.save(user);
    }

    @Transactional
    public Map<String, Object> verifyEkyc(User user, Map<String, Object> body) {
        String type = body.getOrDefault("ekycType", "AADHAAR").toString();
        String idNumber = body.getOrDefault("idNumber", "XXXX-XXXX-8834").toString();

        user.setVerification(VerificationStatus.VERIFIED);
        user.setVerificationBadge(user.getRole() == Role.BUYER ? "VERIFIED_BUYER" : "EKYC_VERIFIED_FARMER");
        user.setKycVerified(true);
        user.setKycVerifiedAt(LocalDateTime.now());
        user.setEkycStatus("VERIFIED");
        user.setEkycType(type);
        user.setEkycIdNumber(idNumber);
        user.setEkycVerifiedAt(LocalDateTime.now());
        if (idNumber.length() >= 4) {
            user.setAadhaarLast4(idNumber.substring(idNumber.length() - 4));
        }

        try {
            user.setEkycDetails(objectMapper.writeValueAsString(body));
        } catch (Exception ignored) {}

        user = userRepository.save(user);
        return sanitizeUser(user);
    }

    public Map<String, Object> getProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return sanitizeUser(user);
    }

    public Map<String, Object> sanitizeUser(User u) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", u.getId());
        map.put("_id", u.getId().toString());
        map.put("name", u.getName());
        map.put("email", u.getEmail());
        map.put("role", u.getRole().name());
        map.put("phone", u.getPhone());
        map.put("location", u.getLocation());
        map.put("address", u.getAddress());
        map.put("district", u.getDistrict());
        map.put("state", u.getState());
        map.put("pincode", u.getPincode());
        map.put("organizationName", u.getOrganizationName());
        map.put("buyerType", u.getBuyerType());
        map.put("farmName", u.getFarmName());
        map.put("landSize", u.getLandSize());
        map.put("primaryCrop", u.getPrimaryCrop());
        map.put("verification", u.getVerification().name());
        map.put("verificationBadge", u.getVerificationBadge());
        map.put("kycVerified", Boolean.TRUE.equals(u.getKycVerified()));
        map.put("aadhaarLast4", u.getAadhaarLast4() != null ? u.getAadhaarLast4() : "8834");
        map.put("ekycStatus", u.getEkycStatus() != null ? u.getEkycStatus() :
                (u.getVerification() == VerificationStatus.VERIFIED && u.getRole() == Role.FARMER ? "VERIFIED" : "NOT_STARTED"));
        map.put("profilePhoto", u.getProfilePhoto());
        map.put("businessVerified", Boolean.TRUE.equals(u.getBusinessVerified()));
        map.put("active", Boolean.TRUE.equals(u.getActive()));
        map.put("geo", Map.of("type", "Point", "coordinates",
                List.of(u.getLongitude() != null ? u.getLongitude() : 75.86,
                        u.getLatitude() != null ? u.getLatitude() : 22.72)));

        if (u.getQualitySpecsJson() != null) {
            try {
                map.put("qualitySpecs", objectMapper.readValue(u.getQualitySpecsJson(), Map.class));
            } catch (Exception ignored) {
                map.put("qualitySpecs", null);
            }
        } else {
            map.put("qualitySpecs", null);
        }
        return map;
    }
}
