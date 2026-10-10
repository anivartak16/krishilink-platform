package com.krishilink.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "logistics_providers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogisticsProvider {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String phone;
    private String vehicleType;
    private Double capacity;

    @Column(columnDefinition = "TEXT")
    private String serviceAreas;

    private Double pricePerKm;

    @Builder.Default
    private Boolean availability = true;

    @JsonProperty("serviceAreas")
    public List<String> getServiceAreasList() {
        if (serviceAreas == null || serviceAreas.isBlank()) return List.of();
        return Arrays.stream(serviceAreas.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @JsonProperty("_id")
    public String getMongoStyleId() {
        return id != null ? id.toString() : null;
    }
}
