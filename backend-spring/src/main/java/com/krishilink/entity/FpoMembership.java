package com.krishilink.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "fpo_memberships",
        uniqueConstraints = @UniqueConstraint(name = "uk_fpo_farmer", columnNames = {"fpo_id", "farmer_id"}))
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FpoMembership {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fpo_id")
    private User fpo;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "farmer_id")
    private User farmer;

    @Builder.Default
    private Boolean ekycVerified = false;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @JsonProperty("_id")
    public String getMongoStyleId() {
        return id == null ? null : id.toString();
    }
}
