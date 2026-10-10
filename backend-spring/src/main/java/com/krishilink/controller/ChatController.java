package com.krishilink.controller;

import com.krishilink.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> chat(@RequestBody Map<String, String> body) {
        String message = body.getOrDefault("message", "").toLowerCase();
        String reply = "KrishiLink AI Assistant: I can help you check mandi prices, crop grading standards, and find verified buyers across Madhya Pradesh and India.";

        if (message.contains("price") || message.contains("wheat") || message.contains("mandi")) {
            reply = "Wheat prices in Malwa mandis (Indore, Dewas, Ujjain) are currently averaging ₹2,420 - ₹2,550/quintal. Check the Mandi Rates tab for live comparisons!";
        } else if (message.contains("organic") || message.contains("certif")) {
            reply = "KrishiLink supports PGS-India and NPOP certified organic produce. Buyers pay up to 15-20% premium for verified organic lots.";
        } else if (message.contains("kyc") || message.contains("aadhaar")) {
            reply = "Complete your Aadhaar e-KYC in your profile to get the Verified Farmer badge and sell directly to top enterprise buyers with escrow guarantee.";
        }

        return ResponseEntity.ok(ApiResponse.ok(Map.of("reply", reply)));
    }
}
