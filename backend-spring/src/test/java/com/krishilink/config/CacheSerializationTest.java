package com.krishilink.config;

import com.krishilink.dto.PriceDtos.PriceHistoryPoint;
import com.krishilink.dto.PriceDtos.PriceInsightDto;
import com.krishilink.entity.MspBenchmark;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CacheSerializationTest {

    @Test
    void priceInsightRoundTripsThroughRedisSerializer() {
        PriceInsightDto insight = PriceInsightDto.builder()
                .commodity("Wheat").currentPrice(24.5).average30Days(23.1).trend("UP")
                .lastUpdated(LocalDateTime.of(2026, 11, 23, 10, 0))
                .history(List.of(PriceHistoryPoint.builder().modalPrice(24.5).date(LocalDate.of(2026, 11, 22)).build()))
                .build();
        var serializer = CacheConfig.serializerFor(CacheConfig.cacheValueTypes().get(CacheConfig.PRICE_INSIGHT));

        PriceInsightDto copy = (PriceInsightDto) serializer.deserialize(serializer.serialize(insight));

        assertThat(copy).usingRecursiveComparison().isEqualTo(insight);
    }

    @Test
    void mspBenchmarkListRoundTripsWithoutTypeMetadata() {
        List<MspBenchmark> list = List.of(MspBenchmark.builder().id(7L).commodity("Wheat")
                .mspPerKg(22.75).mspPerQuintal(2275.0).effectiveDate(LocalDate.of(2026, 4, 1)).build());
        var serializer = CacheConfig.serializerFor(CacheConfig.cacheValueTypes().get(CacheConfig.MSP_BENCHMARKS));

        byte[] bytes = serializer.serialize(list);

        assertThat(new String(bytes)).doesNotContain("@class").contains("\"_id\":\"7\"");
        assertThat((List<?>) serializer.deserialize(bytes)).singleElement()
                .usingRecursiveComparison().isEqualTo(list.get(0));
    }
}
