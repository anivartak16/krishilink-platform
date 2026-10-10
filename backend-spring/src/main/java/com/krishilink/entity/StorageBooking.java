package com.krishilink.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "storage_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StorageBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "warehouse_id")
    private Warehouse warehouse;

    private Double quantity;
    private Integer days;
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
