package com.krishilink;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.krishilink.config.CacheConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class ApiVersionAndActuatorIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CacheManager cacheManager;

    @Test
    @DisplayName("Endpoints are served under /api/v1; unversioned /api paths are marked deprecated")
    void v1PathsAndLegacyDeprecation() throws Exception {
        mockMvc.perform(get("/api/v1/health"))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Deprecation"));
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(header().string("Deprecation", "true"))
                .andExpect(header().string("Link", "</api/v1/health>; rel=\"successor-version\""));
        assertThat(token("/api/v1/auth/login", "buyer@krishilink.com")).isNotBlank();
    }

    @Test
    @DisplayName("Security rules apply identically to /api/v1 paths")
    void v1PathsAreSecuredLikeLegacy() throws Exception {
        String buyer = token("/api/auth/login", "buyer@krishilink.com");
        mockMvc.perform(get("/api/v1/prices").param("commodity", "Wheat")).andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/transactions")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/admin/msp").header("Authorization", "Bearer " + buyer))
                .andExpect(status().isForbidden());
        mockMvc.perform(patch("/api/v1/payments/1").header("Authorization", "Bearer " + buyer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PAID\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Actuator exposes only health and info; health details and other endpoints need admin")
    void actuatorExposure() throws Exception {
        mockMvc.perform(get("/actuator/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"))
                .andExpect(jsonPath("$.components").doesNotExist());
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/info"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.app.api-version").value("v1"));

        mockMvc.perform(get("/actuator/env")).andExpect(status().isUnauthorized());
        String buyer = token("/api/auth/login", "buyer@krishilink.com");
        mockMvc.perform(get("/actuator/env").header("Authorization", "Bearer " + buyer))
                .andExpect(status().isForbidden());

        String admin = token("/api/auth/login", "admin@krishilink.com");
        mockMvc.perform(get("/actuator/health").header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.components.db.status").value("UP"));
        for (String hidden : new String[]{"/actuator/env", "/actuator/beans", "/actuator/metrics", "/actuator/heapdump"}) {
            mockMvc.perform(get(hidden).header("Authorization", "Bearer " + admin)).andExpect(status().isNotFound());
        }
    }

    @Test
    @DisplayName("Price insight is cached per commodity and MSP updates evict MSP caches")
    void marketPriceCaching() throws Exception {
        Cache insights = cacheManager.getCache(CacheConfig.PRICE_INSIGHT);
        Cache benchmarks = cacheManager.getCache(CacheConfig.MSP_BENCHMARKS);
        insights.clear();

        mockMvc.perform(get("/api/v1/prices").param("commodity", " WHEAT ")).andExpect(status().isOk());
        assertThat(insights.get("wheat")).isNotNull();

        mockMvc.perform(get("/api/v1/prices/msp")).andExpect(status().isOk());
        assertThat(benchmarks.get("all")).isNotNull();

        String admin = token("/api/auth/login", "admin@krishilink.com");
        mockMvc.perform(put("/api/v1/admin/msp/Wheat").header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"mspPerQuintal\": 2275}"))
                .andExpect(status().isOk());
        assertThat(benchmarks.get("all")).isNull();
    }

    private String token(String loginPath, String email) throws Exception {
        MvcResult result = mockMvc.perform(post(loginPath)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("email", email, "password", "Demo@12345"))))
                .andExpect(status().isOk())
                .andReturn();
        Map<?, ?> data = (Map<?, ?>) objectMapper.readValue(result.getResponse().getContentAsString(), Map.class).get("data");
        return (String) data.get("token");
    }
}
