package com.example.jejugilmoa.domain.recommendation.entity;

import com.example.jejugilmoa.global.entity.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "recommended_course",
        indexes = {
                @Index(name = "idx_recommended_course_theme", columnList = "theme"),
                @Index(name = "idx_recommended_course_region", columnList = "region")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RecommendedCourse extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(length = 200, nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String theme;

    @Column(length = 100)
    private String region;

    @Column(nullable = false)
    @Builder.Default
    private Integer copyCount = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer shareCount = 0;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(length = 200)
    private String tags;

    @Column(name = "estimated_minutes")
    private Integer estimatedMinutes;

    @Column(name = "transport_mode", length = 20)
    private String transportMode;

    @OneToMany(mappedBy = "course", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sequenceOrder ASC")
    @Builder.Default
    private List<RecommendedCoursePath> paths = new ArrayList<>();
}
