package com.krishilink.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.krishilink.enums.OfferStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "lot_id", nullable = false)
    private Lot lot;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "buyer_id", nullable = false)
    private User buyer;

    @Column(nullable = false)
    private Double quantity;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal pricePerUnit;

    @Column(precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(columnDefinition = "TEXT")
    private String message;

    private LocalDate validUntil;

    @Enumerated(EnumType.STRING)
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
}
