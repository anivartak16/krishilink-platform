package com.krishilink.tradeservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.krishilink.tradeservice.enums.TransactionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long offerId;
    private Long lotId;
    private String commodity;

    private Long buyerId;
    private String buyerName;

    private Long sellerId;
    private String sellerName;

    private Double quantity;
    private Double amount;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TransactionStatus status = TransactionStatus.CREATED;

    @Column(columnDefinition = "TEXT")
    private String eventsJson;

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
        return Map.of("id", buyerId, "_id", buyerId.toString(), "name", buyerName != null ? buyerName : "Buyer");
    }

    @JsonProperty("seller")
    public Map<String, Object> getSellerMap() {
        if (sellerId == null) return null;
        return Map.of("id", sellerId, "_id", sellerId.toString(), "name", sellerName != null ? sellerName : "Farmer");
    }

    @JsonProperty("lot")
    public Map<String, Object> getLotMap() {
        if (lotId == null) return null;
        return Map.of("id", lotId, "_id", lotId.toString(), "commodity", commodity != null ? commodity : "Produce");
    }
}
