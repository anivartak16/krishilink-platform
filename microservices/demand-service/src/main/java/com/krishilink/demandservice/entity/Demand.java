package com.krishilink.demandservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.krishilink.demandservice.enums.DemandStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "demands")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Demand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long buyerId;
    private String buyerName;
    private String buyerOrganization;
    private String buyerPhone;

    @Column(nullable = false)
    private String commodity;

    @Column(nullable = false)
    private Double requiredQuantity;

    @Builder.Default
    private String unit = "KG";

    private String requiredQuality;
    private String variety;
    private Double maxMoisture;
    private String preferredLocation;
    private String deliveryLocation;

    @Column(nullable = false)
    private Double maxPrice;

    private String paymentTerms;
    private String contactPhone;
    private String farmingType;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DemandStatus status = DemandStatus.ACTIVE;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @JsonProperty("_id")
    public String getMongoStyleId() {
        return id != null ? id.toString() : null;
    }

    @JsonProperty("buyer")
    public Map<String, Object> getBuyerMap() {
        if (buyerId == null) return null;
        return Map.of(
                "id", buyerId,
                "_id", buyerId.toString(),
                "name", buyerName != null ? buyerName : "Buyer",
                "organizationName", buyerOrganization != null ? buyerOrganization : "AgroCorp",
                "phone", buyerPhone != null ? buyerPhone : ""
        );
    }
}
