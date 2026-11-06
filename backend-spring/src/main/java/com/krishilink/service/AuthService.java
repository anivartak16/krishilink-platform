package com.krishilink.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishilink.dto.AuthDtos.*;
import com.krishilink.entity.User;
import com.krishilink.enums.Role;
import com.krishilink.enums.VerificationStatus;
import com.krishilink.exception.BadRequestException;
import com.krishilink.repository.TransactionRepository;
import com.krishilink.repository.UserRepository;
import com.krishilink.security.JwtUtils;
import com.krishilink.util.GeoUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final TransactionRepository transactionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;
    private final ObjectMapper objectMapper;

    @Transactional
    public AuthResponse register(RegisterRequest req) {
        if (req.getName() == null || req.getEmail() == null || req.getPassword() == null ||
                req.getPhone() == null || req.getLocation() == null || req.getRole() == null) {
            throw new BadRequestException("Name, email, phone, location, password and valid role are required");
        }

        Role role;
        try {
            role = Role.valueOf(req.getRole().toUpperCase().trim());
        } catch (Exception e) {
            throw new BadRequestException("Invalid role specified");
        }

        if (userRepository.existsByEmailIgnoreCase(req.getEmail().trim())) {
            throw new BadRequestException("Email already registered");
        }

        List<Double> coords;
        if (req.getLongitude() != null && req.getLatitude() != null) {
            coords = List.of(req.getLongitude(), req.getLatitude());
        } else {
            coords = GeoUtils.resolveCoordinates(req.getDistrict(), req.getState(), req.getLocation());
        }

        String cropsStr = null;
        if (req.getCrops() != null && !req.getCrops().isEmpty()) {
            cropsStr = String.join(",", req.getCrops());
        } else if (req.getPrimaryCrop() != null) {
            cropsStr = req.getPrimaryCrop();
        }

        String reqCropsStr = null;
        if (req.getRequiredCrops() != null && !req.getRequiredCrops().isEmpty()) {
            reqCropsStr = String.join(",", req.getRequiredCrops());
        }

        String qualitySpecsJson = null;
        if (req.getQualitySpecs() != null) {
            try {
                qualitySpecsJson = objectMapper.writeValueAsString(req.getQualitySpecs());
            } catch (Exception ignored) {}
        }

        User user = User.builder()
                .name(req.getName().trim())
                .email(req.getEmail().trim().toLowerCase())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .phone(req.getPhone().trim())
                .location(req.getLocation().trim())
                .address(req.getAddress())
                .district(req.getDistrict())
                .state(req.getState())
                .pincode(req.getPincode())
                .latitude(coords.get(1))
                .longitude(coords.get(0))
                .farmName(req.getFarmName())
                .landSize(req.getLandSize())
                .primaryCrop(req.getPrimaryCrop())
                .crops(cropsStr)
                .availableQuantity(req.getAvailableQuantity())
                .cropQuality(req.getCropQuality() != null ? req.getCropQuality() : "Grade A")
                .fpoAssociation(req.getFpoAssociation())
                .organizationName(req.getOrganizationName())
                .buyerType(req.getBuyerType())
                .requiredCrops(reqCropsStr)
                .requiredQuantity(req.getRequiredQuantity())
                .qualityRequirements(req.getQualityRequirements())
                .qualitySpecsJson(qualitySpecsJson)
                .registrationNumber(req.getRegistrationNumber())
                .memberCount(req.getMemberCount())
                .verification(VerificationStatus.UNVERIFIED)
                .verificationBadge("STANDARD")
                .tradeRating(4.5)
                .build();

        user = userRepository.save(user);

        String token = jwtUtils.generateToken(user.getId());
        return AuthResponse.builder()
                .token(token)
                .user(sanitizeUser(user))
                .build();
    }

    public AuthResponse login(LoginRequest req) {
        if (req.getEmail() == null || req.getPassword() == null) {
            throw new BadCredentialsException("Email and password are required");
        }

        String cleanEmail = req.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmailIgnoreCase(cleanEmail);

        if (userOpt.isEmpty()) {
            if (cleanEmail.endsWith("@krishilink.com")) {
                userOpt = userRepository.findByEmailIgnoreCase(cleanEmail.replace("@krishilink.com", "@agrilink.com"));
            } else if (cleanEmail.endsWith("@agrilink.com")) {
                userOpt = userRepository.findByEmailIgnoreCase(cleanEmail.replace("@agrilink.com", "@krishilink.com"));
            }
        }

        User user = userOpt.orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtUtils.generateToken(user.getId());
        return AuthResponse.builder()
                .token(token)
                .user(sanitizeUser(user))
                .build();
    }

    @Transactional
    public Map<String, Object> updateProfile(User user, UpdateProfileRequest req) {
        if (req.getName() != null) user.setName(req.getName());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getLocation() != null) user.setLocation(req.getLocation());
        if (req.getAddress() != null) user.setAddress(req.getAddress());
        if (req.getDistrict() != null) user.setDistrict(req.getDistrict());
        if (req.getState() != null) user.setState(req.getState());
        if (req.getPincode() != null) user.setPincode(req.getPincode());
        if (req.getOrganizationName() != null) user.setOrganizationName(req.getOrganizationName());
        if (req.getBuyerType() != null) user.setBuyerType(req.getBuyerType());
        if (req.getRegistrationNumber() != null) user.setRegistrationNumber(req.getRegistrationNumber());
        if (req.getMemberCount() != null) user.setMemberCount(req.getMemberCount());
        if (req.getPrimaryCrop() != null) user.setPrimaryCrop(req.getPrimaryCrop());
        if (req.getAvailableQuantity() != null) user.setAvailableQuantity(req.getAvailableQuantity());
        if (req.getCropQuality() != null) user.setCropQuality(req.getCropQuality());
        if (req.getFpoAssociation() != null) user.setFpoAssociation(req.getFpoAssociation());
        if (req.getRequiredQuantity() != null) user.setRequiredQuantity(req.getRequiredQuantity());
        if (req.getQualityRequirements() != null) user.setQualityRequirements(req.getQualityRequirements());
        if (req.getProfilePhoto() != null) user.setProfilePhoto(req.getProfilePhoto());
        if (req.getFarmName() != null) user.setFarmName(req.getFarmName());
        if (req.getLandSize() != null) user.setLandSize(req.getLandSize());
        if (req.getGstNumber() != null) user.setGstNumber(req.getGstNumber());
        if (req.getPanNumber() != null) user.setPanNumber(req.getPanNumber());
        if (req.getMandiLicenseNumber() != null) user.setMandiLicenseNumber(req.getMandiLicenseNumber());

        if (req.getQualitySpecs() != null) {
            try {
                user.setQualitySpecsJson(objectMapper.writeValueAsString(req.getQualitySpecs()));
            } catch (Exception ignored) {}
        }

        if (req.getCrops() != null) {
            if (req.getCrops() instanceof List<?> list) {
                user.setCrops(String.join(",", list.stream().map(Object::toString).toList()));
            } else {
                user.setCrops(req.getCrops().toString());
            }
        }

        if (req.getRequiredCrops() != null) {
            if (req.getRequiredCrops() instanceof List<?> list) {
                user.setRequiredCrops(String.join(",", list.stream().map(Object::toString).toList()));
            } else {
                user.setRequiredCrops(req.getRequiredCrops().toString());
            }
        }

        if (user.getRole() == Role.BUYER && (user.getGstNumber() != null || user.getMandiLicenseNumber() != null)) {
            user.setVerification(VerificationStatus.VERIFIED);
            user.setVerificationBadge("VERIFIED_BUYER");
        }

        List<Double> coords = GeoUtils.resolveCoordinates(user.getDistrict(), user.getState(), user.getLocation());
        user.setLongitude(coords.get(0));
        user.setLatitude(coords.get(1));

        user = userRepository.save(user);
        return sanitizeUser(user);
    }

    public Map<String, Object> verifyEkyc(User user, Map<String, Object> body) {
        String ekycType = body.get("ekycType") != null ? body.get("ekycType").toString() : "KISAN_CREDIT_CARD";
        String idNumber = body.get("idNumber") != null ? body.get("idNumber").toString() : "";
        String fullName = body.get("fullName") != null ? body.get("fullName").toString() : user.getName();

        user.setEkycType(ekycType);
        user.setEkycIdNumber(idNumber);
        user.setEkycStatus("VERIFIED");
        user.setEkycVerifiedAt(LocalDateTime.now());
        user.setVerification(VerificationStatus.VERIFIED);
        user.setVerificationBadge(user.getRole() == Role.BUYER ? "VERIFIED_BUYER" : "EKYC_VERIFIED_FARMER");

        Map<String, Object> details = new LinkedHashMap<>();
        details.put("fullName", fullName);
        details.put("idNumber", idNumber);
        details.put("docType", ekycType);
        details.put("landRecordNumber", body.get("landRecordNumber"));
        details.put("village", body.get("village"));
        details.put("verifiedAt", LocalDateTime.now().toString());
        try {
            user.setEkycDetails(objectMapper.writeValueAsString(details));
        } catch (Exception ignored) {}

        user = userRepository.save(user);
        return sanitizeUser(user);
    }

    public Map<String, Object> getProfile(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new BadRequestException("User not found"));

        Map<String, Object> sanitized = sanitizeUser(user);
        long txCount = transactionRepository.countCompletedByUser(user, com.krishilink.enums.TransactionStatus.COMPLETED);
        sanitized.put("transactionCount", txCount);
        return sanitized;
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
        map.put("crops", u.getCropsList());
        map.put("availableQuantity", u.getAvailableQuantity());
        map.put("cropQuality", u.getCropQuality());
        map.put("fpoAssociation", u.getFpoAssociation());
        map.put("requiredCrops", u.getRequiredCropsList());
        map.put("requiredQuantity", u.getRequiredQuantity());
        map.put("qualityRequirements", u.getQualityRequirements());
        map.put("registrationNumber", u.getRegistrationNumber());
        map.put("memberCount", u.getMemberCount());
        map.put("fpoLeaderDesignation", u.getFpoLeaderDesignation());
        map.put("fpoEstablishmentYear", u.getFpoEstablishmentYear());
        map.put("fpoIncorporationType", u.getFpoIncorporationType());
        map.put("fpoAggregationCapacity", u.getFpoAggregationCapacity());
        map.put("buyerCapacity", u.getBuyerCapacity());
        map.put("verification", u.getVerification().name());
        map.put("verificationBadge", u.getVerificationBadge() != null ? u.getVerificationBadge() :
                (u.getVerification() == VerificationStatus.VERIFIED ?
                        (u.getRole() == Role.BUYER ? "VERIFIED_BUYER" : "EKYC_VERIFIED_FARMER") : "STANDARD"));
        map.put("gstNumber", u.getGstNumber());
        map.put("panNumber", u.getPanNumber());
        map.put("mandiLicenseNumber", u.getMandiLicenseNumber());
        map.put("tradeRating", u.getTradeRating() != null ? u.getTradeRating() : 4.5);
        map.put("kycVerified", Boolean.TRUE.equals(u.getKycVerified()));
        map.put("kycVerifiedAt", u.getKycVerifiedAt());
        map.put("aadhaarLast4", u.getAadhaarLast4());
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
