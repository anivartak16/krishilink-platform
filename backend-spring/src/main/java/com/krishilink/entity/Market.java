package com.krishilink.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "markets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Market {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    private String location;
    private String district;
    private String state;

    @Column(columnDefinition = "TEXT")
    private String commodities;

    private Double latitude;
    private Double longitude;

    @Builder.Default
    private Double transportCostPerKm = 8.0;

    @Builder.Default
    private Double reviewAverage = 4.2;

    @Builder.Default
    private Integer reviewCount = 0;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @JsonProperty("geo")
    public Map<String, Object> getGeo() {
        if (longitude != null && latitude != null) {
            return Map.of("type", "Point", "coordinates", List.of(longitude, latitude));
        }
        return Map.of("type", "Point", "coordinates", List.of(75.86, 22.72));
    }

    @JsonProperty("commodities")
    public List<String> getCommoditiesList() {
        if (commodities == null || commodities.isBlank()) return List.of();
        return Arrays.stream(commodities.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @JsonProperty("_id")
    public String getMongoStyleId() {
        return id != null ? id.toString() : null;
    }
}
