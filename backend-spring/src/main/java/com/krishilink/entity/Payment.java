package com.krishilink.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.krishilink.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "transaction_id", unique = true)
    private Transaction transaction;

    @Column(precision = 14, scale = 2)
    private BigDecimal amount;

    @Builder.Default
    private String paymentMethod = "Bank Transfer";

    private String referenceId;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    private LocalDateTime paidAt;
    private String remarks;

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
