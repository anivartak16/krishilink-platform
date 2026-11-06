package com.krishilink.lotservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishilink.lotservice.dto.LotDto;
import com.krishilink.lotservice.repository.LotRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "eureka.client.register-with-eureka=false",
    "eureka.client.fetch-registry=false"
})
@AutoConfigureMockMvc
class LotServiceTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private LotRepository lotRepository;

    @Test
    @DisplayName("Test Lot Creation and Quality Inspection")
    void testLotAndInspection() throws Exception {
        String farmerToken = io.jsonwebtoken.Jwts.builder()
                .subject("farmer@krishilink.com")
                .claim("id", 2L)
                .claim("role", "FARMER")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor("krishilink-super-secret-key-for-jwt-tokens-must-be-very-long-and-secure-32bytes".getBytes()))
                .compact();

        LotDto dto = LotDto.builder()
                .commodity("Mustard")
                .quantity(1500.0)
                .expectedPrice(5200.0)
                .location("Ujjain")
                .grade("Grade A")
                .build();

        mockMvc.perform(post("/api/lots")
                        .header("Authorization", "Bearer " + farmerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING_VERIFICATION"));

        // Get lots with mine=true
        mockMvc.perform(get("/api/lots?mine=true")
                        .header("Authorization", "Bearer " + farmerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
