package com.krishilink.tradeservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.krishilink.tradeservice.enums.OfferStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "offers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Offer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long lotId;
    private String commodity;

    private Long buyerId;
    private String buyerName;
    private String buyerPhone;

    private Long sellerId;
    private String sellerName;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false)
    private Double pricePerUnit;

    private Double totalAmount;
    private String message;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OfferStatus status = OfferStatus.PENDING;

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
        return Map.of("id", buyerId, "_id", buyerId.toString(), "name", buyerName != null ? buyerName : "Buyer", "phone", buyerPhone != null ? buyerPhone : "");
    }

    @JsonProperty("lot")
    public Map<String, Object> getLotMap() {
        if (lotId == null) return null;
        return Map.of("id", lotId, "_id", lotId.toString(), "commodity", commodity != null ? commodity : "Produce");
    }
}
