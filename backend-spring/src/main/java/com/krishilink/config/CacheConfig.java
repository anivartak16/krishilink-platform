package com.krishilink.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.krishilink.dto.PriceDtos.PriceInsightDto;
import com.krishilink.dto.PriceDtos.PriceOpportunityAlertDto;
import com.krishilink.dto.PriceDtos.SellAdviceDto;
import com.krishilink.entity.MspBenchmark;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.Cache;
import org.springframework.cache.annotation.CachingConfigurer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheErrorHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Market-price caches. Backed by Redis when spring.cache.type=redis, otherwise in-memory.
 * Each cache has a typed JSON serializer, so Redis never holds class names that drive deserialization.
 * Cache errors (e.g. Redis down) are logged and the call falls through to the database.
 */
@Configuration
@EnableCaching
@Slf4j
public class CacheConfig implements CachingConfigurer {

    public static final String PRICE_INSIGHT = "priceInsight";
    public static final String PRICE_OPPORTUNITY = "priceOpportunity";
    public static final String SELL_ADVICE = "sellAdvice";
    public static final String MSP = "msp";
    public static final String MSP_BENCHMARKS = "mspBenchmarks";

    public static final String COMMODITY_KEY = "#commodity == null ? '' : #commodity.trim().toLowerCase()";

    static final ObjectMapper CACHE_MAPPER = JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();

    static Map<String, JavaType> cacheValueTypes() {
        return Map.of(
                PRICE_INSIGHT, CACHE_MAPPER.constructType(PriceInsightDto.class),
                PRICE_OPPORTUNITY, CACHE_MAPPER.constructType(PriceOpportunityAlertDto.class),
                SELL_ADVICE, CACHE_MAPPER.constructType(SellAdviceDto.class),
                MSP, CACHE_MAPPER.constructType(Double.class),
                MSP_BENCHMARKS, CACHE_MAPPER.getTypeFactory().constructCollectionType(List.class, MspBenchmark.class));
    }

    static Jackson2JsonRedisSerializer<Object> serializerFor(JavaType type) {
        return new Jackson2JsonRedisSerializer<>(CACHE_MAPPER, type);
    }

    @Bean
    public RedisCacheManagerBuilderCustomizer marketPriceRedisCaches(
            @Value("${krishilink.cache.price-ttl:PT10M}") Duration priceTtl,
            @Value("${krishilink.cache.msp-ttl:PT6H}") Duration mspTtl) {
        return builder -> cacheValueTypes().forEach((name, type) -> builder.withCacheConfiguration(name,
                RedisCacheConfiguration.defaultCacheConfig()
                        .prefixCacheNameWith("krishilink:")
                        .entryTtl(name.startsWith("msp") ? mspTtl : priceTtl)
                        .disableCachingNullValues()
                        .serializeValuesWith(SerializationPair.fromSerializer(serializerFor(type)))));
    }

    @Override
    public CacheErrorHandler errorHandler() {
        return new CacheErrorHandler() {
            @Override
            public void handleCacheGetError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache get failed on {}[{}]: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCachePutError(RuntimeException e, Cache cache, Object key, Object value) {
                log.warn("Cache put failed on {}[{}]: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheEvictError(RuntimeException e, Cache cache, Object key) {
                log.warn("Cache evict failed on {}[{}]: {}", cache.getName(), key, e.getMessage());
            }

            @Override
            public void handleCacheClearError(RuntimeException e, Cache cache) {
                log.warn("Cache clear failed on {}: {}", cache.getName(), e.getMessage());
            }
        };
    }
}
