package com.example.jejugilmoa.domain.recommendation.entity;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "recommended_course_path",
        indexes = {
                @Index(name = "idx_course_path_course", columnList = "course_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_course_path_sequence",
                        columnNames = {"course_id", "sequence_order"}
                )
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class RecommendedCoursePath extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private RecommendedCourse course;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(name = "sequence_order", nullable = false)
    private Integer sequenceOrder;

    @Column(name = "recommended_duration")
    private Integer recommendedDuration;

    @Column(name = "travel_time_to_next")
    private Integer travelTimeToNext;

    @Column(name = "travel_mode_to_next", length = 20)
    private String travelModeToNext;
}
