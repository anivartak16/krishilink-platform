package com.krishilink.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "market_prices", indexes = {
        @Index(name = "idx_mp_commodity", columnList = "commodity"),
        @Index(name = "idx_mp_date", columnList = "priceDate")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MarketPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "market_id")
    private Market market;

    @Column(nullable = false)
    private String commodity;

    private String variety;
    private String grade;

    private LocalDate priceDate;

    private Double minPrice;
    private Double maxPrice;
    private Double modalPrice;
    private Double arrivalVolume;

    @Builder.Default
    private String unit = "KG";

    private String state;
    private String district;
    private String marketName;

    @Builder.Default
    private String source = "AGMARKNET";

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @JsonProperty("date")
    public LocalDate getDate() {
        return priceDate;
    }

    @JsonProperty("arrivalDate")
    public LocalDate getArrivalDate() {
        return priceDate;
    }

    @JsonProperty("_id")
    public String getMongoStyleId() {
        return id != null ? id.toString() : null;
    }
}
