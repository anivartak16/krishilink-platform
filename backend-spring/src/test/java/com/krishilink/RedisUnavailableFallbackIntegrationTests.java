package com.krishilink;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Redis configured but unreachable: price endpoints must still answer from the database. */
@SpringBootTest(properties = {"spring.cache.type=redis", "spring.data.redis.url=redis://localhost:1"})
@AutoConfigureMockMvc
class RedisUnavailableFallbackIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void priceEndpointsFallBackToDatabase() throws Exception {
        mockMvc.perform(get("/api/v1/prices").param("commodity", "Wheat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commodity").value("Wheat"));
        mockMvc.perform(get("/api/v1/prices/msp")).andExpect(status().isOk());
    }
}
