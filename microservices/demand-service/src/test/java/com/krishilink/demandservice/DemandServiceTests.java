package com.krishilink.demandservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishilink.demandservice.dto.DemandDto;
import com.krishilink.demandservice.repository.DemandRepository;
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
class DemandServiceTests {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private DemandRepository demandRepository;

    @Test
    @DisplayName("Test Demand Creation and Retrieval")
    void testDemands() throws Exception {
        String buyerToken = io.jsonwebtoken.Jwts.builder()
                .subject("buyer@krishilink.com")
                .claim("id", 3L)
                .claim("role", "BUYER")
                .signWith(io.jsonwebtoken.security.Keys.hmacShaKeyFor("krishilink-super-secret-key-for-jwt-tokens-must-be-very-long-and-secure-32bytes".getBytes()))
                .compact();

        DemandDto dto = DemandDto.builder()
                .commodity("Maize")
                .requiredQuantity(4000.0)
                .maxPrice(2100.0)
                .preferredLocation("Indore")
                .build();

        mockMvc.perform(post("/api/demands")
                        .header("Authorization", "Bearer " + buyerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(dto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.commodity").value("Maize"));

        mockMvc.perform(get("/api/demands?mine=true")
                        .header("Authorization", "Bearer " + buyerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());
    }
}
