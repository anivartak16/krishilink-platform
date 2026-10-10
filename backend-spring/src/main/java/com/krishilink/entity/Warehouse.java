package com.krishilink.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

import java.util.Arrays;
import java.util.List;

@Entity
@Table(name = "warehouses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
    private String location;
    private Double availableCapacity;
    private String storageType;
    private Double pricePerUnitPerDay;

    @Column(columnDefinition = "TEXT")
    private String facilities;

    @Builder.Default
    private String verificationStatus = "VERIFIED";

    @JsonProperty("facilities")
    public List<String> getFacilitiesList() {
        if (facilities == null || facilities.isBlank()) return List.of();
        return Arrays.stream(facilities.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    @JsonProperty("_id")
    public String getMongoStyleId() {
        return id != null ? id.toString() : null;
    }
}
