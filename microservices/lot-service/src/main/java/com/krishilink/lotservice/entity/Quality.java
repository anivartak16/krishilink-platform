package com.krishilink.lotservice.entity;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.persistence.*;
import lombok.*;

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
    @Builder.Default
    private String grade = "Grade A";

    private Double moisture;
    private Double foreignMatter;
    private Double damagedPercentage;
    private String certification;
    private String defects;

    @Column(columnDefinition = "TEXT")
    private String grainImage;

    @Column(columnDefinition = "TEXT")
    private String inspectionNotes;

    private String inspectionStatus;
    private String inspectedByName;
    private LocalDateTime inspectedAt;

    @JsonProperty("_id")
    public String getMongoStyleId() {
        return id != null ? id.toString() : null;
    }
}
