package com.krishilink.marketservice.controller;

import com.krishilink.marketservice.dto.ApiResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, String>>> chat(@RequestBody Map<String, String> body) {
        String msg = body.getOrDefault("message", "").toLowerCase();
        String reply = "KrishiLink AI Kendra Advisor: I can assist with Mandi prices, crop grading, verified institutional buyers, and escrow trade protections across India.";
        if (msg.contains("price") || msg.contains("wheat")) {
            reply = "Wheat in Malwa mandis is trading between ₹2,420 and ₹2,550/quintal. Recommended strategy: Hold for 8 days for projected peak.";
        }
        return ResponseEntity.ok(ApiResponse.ok(Map.of("reply", reply)));
    }
}
