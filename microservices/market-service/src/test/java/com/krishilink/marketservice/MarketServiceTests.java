package com.krishilink.marketservice;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "eureka.client.register-with-eureka=false",
    "eureka.client.fetch-registry=false"
})
@AutoConfigureMockMvc
class MarketServiceTests {

    @Autowired private MockMvc mockMvc;

    @Test
    @DisplayName("Test Markets and Market Prices")
    void testMarketPrices() throws Exception {
        mockMvc.perform(get("/api/markets/nearby?commodity=Wheat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        mockMvc.perform(get("/api/marketPrice/prices/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRecords").isNumber());

        mockMvc.perform(get("/api/prices/trends?commodity=Wheat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.history").isArray());

        mockMvc.perform(get("/api/predictions/Wheat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.forecast").isArray());
    }
}
