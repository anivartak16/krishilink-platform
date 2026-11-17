package com.krishilink.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "msp_benchmarks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MspBenchmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String commodity;

    @Column(nullable = false)
    private Double mspPerQuintal;

    @Column(nullable = false)
    private Double mspPerKg;

    private String marketingSeason;

    @Builder.Default
    private Boolean isStatutoryMsp = true;

    private LocalDate effectiveDate;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @JsonProperty("_id")
    public String getMongoStyleId() {
        return id != null ? id.toString() : null;
    }
}
