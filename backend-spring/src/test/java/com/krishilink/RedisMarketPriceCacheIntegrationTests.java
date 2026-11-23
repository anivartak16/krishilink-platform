package com.krishilink;

import com.krishilink.config.CacheConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Runs against a real Redis when REDIS_URL is set (e.g. docker run -p 6379:6379 redis:7-alpine). */
@SpringBootTest(properties = {"spring.cache.type=redis", "management.health.redis.enabled=true"})
@AutoConfigureMockMvc
@EnabledIfEnvironmentVariable(named = "REDIS_URL", matches = ".+")
class RedisMarketPriceCacheIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate redis;

    @Test
    void priceInsightIsStoredInRedisWithTtlAndServedFromIt() throws Exception {
        assertThat(cacheManager).isInstanceOf(RedisCacheManager.class);
        String key = "krishilink:" + CacheConfig.PRICE_INSIGHT + "::wheat";
        redis.delete(key);

        mockMvc.perform(get("/api/v1/prices").param("commodity", "Wheat")).andExpect(status().isOk());

        assertThat(redis.opsForValue().get(key)).contains("\"commodity\":\"Wheat\"").doesNotContain("@class");
        assertThat(redis.getExpire(key)).isBetween(1L, 600L);

        mockMvc.perform(get("/api/v1/prices").param("commodity", "wheat"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.commodity").value("Wheat"));
        mockMvc.perform(get("/actuator/health")).andExpect(jsonPath("$.status").value("UP"));
    }
}
