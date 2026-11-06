package com.krishilink.userservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishilink.userservice.dto.AuthDtos.*;
import com.krishilink.userservice.entity.User;
import com.krishilink.userservice.enums.Role;
import com.krishilink.userservice.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "eureka.client.register-with-eureka=false",
    "eureka.client.fetch-registry=false"
})
@AutoConfigureMockMvc
class UserServiceTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private UserRepository userRepository;

    @Test
    @DisplayName("Test User Registration, Login, and Profile")
    void testAuthLifecycle() throws Exception {
        String email = "testfarmer_" + System.currentTimeMillis() + "@krishilink.com";
        RegisterRequest reg = RegisterRequest.builder()
                .name("Kisan Test")
                .email(email)
                .password("Demo@12345")
                .role(Role.FARMER)
                .location("Indore")
                .district("Indore")
                .state("Madhya Pradesh")
                .build();

        // 1. Register
        MvcResult regResult = mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.token").isString())
                .andReturn();

        String token = (String) ((Map<?, ?>) objectMapper.readValue(regResult.getResponse().getContentAsString(), Map.class).get("data")).get("token");

        // 2. /api/auth/me
        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));

        // 3. Login
        LoginRequest login = LoginRequest.builder().email(email).password("Demo@12345").build();
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isString());
    }

    @Test
    @DisplayName("Test e-KYC Verification")
    void testEkycFlow() throws Exception {
        // Generate OTP with valid Verhoeff Aadhaar
        MvcResult otpRes = mockMvc.perform(post("/api/auth/kyc/send-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("aadhaarNumber", "367598324576"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.client_id").isString())
                .andReturn();

        String clientId = (String) ((Map<?, ?>) objectMapper.readValue(otpRes.getResponse().getContentAsString(), Map.class).get("data")).get("client_id");

        // Verify OTP
        mockMvc.perform(post("/api/auth/kyc/verify-otp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("client_id", clientId, "otp", "123456"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verified").value(true));
    }
}
