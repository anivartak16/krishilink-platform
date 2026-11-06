package com.krishilink.lotservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.krishilink.lotservice.enums.LotStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "lots")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Lot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long ownerId;
    private String ownerName;
    private String ownerType;
    private String ownerPhone;

    @Column(nullable = false)
    private String commodity;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private Double remainingQuantity;

    @Builder.Default
    private String unit = "KG";

    private LocalDate harvestDate;
    private String location;
    private Double longitude;
    private Double latitude;

    @Column(nullable = false)
    private Double expectedPrice;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "quality_id")
    private Quality quality;

    private LocalDate availableUntil;

    @Builder.Default
    private String farmingType = "conventional";

    @Builder.Default
    private String organicCertificationStatus = "not_verified";

    private String certificationType;
    private String certificateId;
    private String certificateValidity;
    private String certificateDocumentUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private LotStatus status = LotStatus.AVAILABLE;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @JsonProperty("_id")
    public String getMongoStyleId() {
        return id != null ? id.toString() : null;
    }

    @JsonProperty("owner")
    public Map<String, Object> getOwnerMap() {
        if (ownerId == null) return null;
        return Map.of(
                "id", ownerId,
                "_id", ownerId.toString(),
                "name", ownerName != null ? ownerName : "Farmer",
                "phone", ownerPhone != null ? ownerPhone : "",
                "role", ownerType != null ? ownerType : "FARMER"
        );
    }
}
