package com.krishilink.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.krishilink.enums.LotStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "lots", indexes = {
        @Index(name = "idx_lot_commodity", columnList = "commodity"),
        @Index(name = "idx_lot_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(columnDefinition = "bigint default 0")
    private Long version;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Builder.Default
    private String ownerType = "FARMER";

    @Column(nullable = false)
    private String commodity;

    @Column(nullable = false)
    private Double quantity;

    private Double remainingQuantity;

    @Builder.Default
    private String unit = "KG";

    private LocalDate harvestDate;
    private String location;

    private Double latitude;
    private Double longitude;

    @Column(nullable = false)
    private Double expectedPrice;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "quality_id")
    private Quality quality;

    private LocalDate availableUntil;

    @Builder.Default
    private String farmingType = "conventional";

    @Builder.Default
    private String organicCertificationStatus = "not_verified";

    @Builder.Default
    private String certificationType = "";

    private String certificateId;
    private LocalDate certificateValidity;
    private String certificateDocumentUrl;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private LotStatus status = LotStatus.PENDING_VERIFICATION;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @JsonProperty("geo")
    public Map<String, Object> getGeo() {
        if (longitude != null && latitude != null) {
            return Map.of("type", "Point", "coordinates", List.of(longitude, latitude));
        }
        return Map.of("type", "Point", "coordinates", List.of(75.86, 22.72));
    }

    @JsonProperty("_id")
    public String getMongoStyleId() {
        return id != null ? id.toString() : null;
    }
}
