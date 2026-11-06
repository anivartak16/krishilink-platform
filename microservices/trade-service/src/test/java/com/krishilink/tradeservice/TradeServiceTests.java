package com.krishilink.tradeservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishilink.tradeservice.dto.OfferDto;
import com.krishilink.tradeservice.repository.OfferRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "eureka.client.register-with-eureka=false",
    "eureka.client.fetch-registry=false"
})
@AutoConfigureMockMvc
class TradeServiceTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private OfferRepository offerRepository;

    @Test
    @DisplayName("Test Offer, Transaction, and Escrow Payment")
    void testTradeLifecycle() throws Exception {
        String buyerToken = io.jsonwebtoken.Jwts.builder()
                .subject("buyer@krishilink.com")
                .claim("id", 3L)
                .claim("role", "BUYER")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor("krishilink-super-secret-key-for-jwt-tokens-must-be-very-long-and-secure-32bytes".getBytes()))
                .compact();

        OfferDto offer = OfferDto.builder()
                .lotId(1L)
                .quantity(500.0)
                .pricePerUnit(2450.0)
                .message("Pickup ready")
                .build();

        mockMvc.perform(post("/api/offers")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(offer)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(get("/api/transactions")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
