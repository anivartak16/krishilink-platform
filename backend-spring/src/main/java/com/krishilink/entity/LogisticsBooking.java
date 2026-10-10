package com.krishilink.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "logistics_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogisticsBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "provider_id")
    private LogisticsProvider provider;

    private String pickupLocation;
    private String deliveryLocation;
    private Double quantity;
    private Double distanceKm;
    private Double estimatedCost;

    @Builder.Default
    private String status = "REQUESTED";

    @CreationTimestamp
    private LocalDateTime createdAt;

    @JsonProperty("_id")
    public String getMongoStyleId() {
        return id == null ? null : id.toString();
    }
}
