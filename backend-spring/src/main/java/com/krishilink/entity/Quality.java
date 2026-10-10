package com.krishilink.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "qualities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Quality {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String grade;

    private Double moisture;
    private Double foreignMatter;
    private Double damagedPercentage;
    private String certification;
    private String defects;
    @Column(columnDefinition = "TEXT")
    private String inspectionNotes;
    @Column(columnDefinition = "LONGTEXT")
    private String grainImage;
    @Builder.Default
    private String inspectionStatus = "PENDING";
    private LocalDateTime inspectedAt;
    private String inspectedByName;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @JsonProperty("_id")
    public String getMongoStyleId() {
        return id != null ? id.toString() : null;
    }
}
