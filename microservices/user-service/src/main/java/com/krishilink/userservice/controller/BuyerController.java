package com.krishilink.userservice.controller;

import com.krishilink.userservice.dto.ApiResponse;
import com.krishilink.userservice.enums.Role;
import com.krishilink.userservice.repository.UserRepository;
import com.krishilink.userservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/buyers")
@RequiredArgsConstructor
public class BuyerController {

    private final UserRepository userRepository;
    private final AuthService authService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getBuyers() {
        List<Map<String, Object>> buyers = userRepository.findByRole(Role.BUYER).stream()
                .map(authService::sanitizeUser)
                .toList();
        return ResponseEntity.ok(ApiResponse.ok(buyers));
    }
}
