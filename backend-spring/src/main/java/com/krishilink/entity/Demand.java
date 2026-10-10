package com.krishilink.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.krishilink.enums.DemandStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "demands", indexes = {
        @Index(name = "idx_demand_commodity", columnList = "commodity"),
        @Index(name = "idx_demand_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Demand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(nullable = false)
    private String commodity;

    @Column(nullable = false)
    private Double requiredQuantity;

    @Builder.Default
    private String unit = "KG";

    @Builder.Default
    private String requiredQuality = "Grade A";

    @Builder.Default
    private String variety = "All Varieties";

    @Builder.Default
    private Double maxMoisture = 12.0;

    private String preferredLocation;
    private String deliveryLocation;

    @Column(nullable = false)
    private Double maxPrice;

    @Builder.Default
    private String paymentTerms = "100% Escrow Secured";

    private String contactPhone;

    @Builder.Default
    private String farmingType = "any";

    @Column(columnDefinition = "TEXT")
    private String notes;

    private LocalDate deadline;

    @Enumerated(EnumType.STRING)
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
}
