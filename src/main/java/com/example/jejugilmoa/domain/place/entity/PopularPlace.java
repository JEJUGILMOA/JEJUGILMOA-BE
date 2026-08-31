package com.example.jejugilmoa.domain.place.entity;


import com.example.jejugilmoa.domain.place.enums.CurationLabel;
import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;
import java.math.BigDecimal;

@Entity
@Table(name = "popular_place", indexes = {
        @Index(name = "idx_popular_place_place", columnList = "place_id"),
        @Index(name = "idx_popular_place_curation_label", columnList = "curation_label")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_popular_place_place_id", columnNames = "place_id")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PopularPlace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "place_id",
            nullable = false,
            unique = true
    )
    private Place place;

    @Builder.Default
    @Column(nullable = false)
    private int searchCount = 0; // 검색량

    @Builder.Default
    @Column(nullable = false)
    private int visitCount = 0; // 방문자 수

    @Column(precision = 3, scale = 2)
    private BigDecimal rating;  // 평점 (0.00 ~ 5.00)

    @Enumerated(EnumType.STRING)
    @Column(name = "curation_label", length = 30)
    private CurationLabel curationLabel;

    public void updateVisitCount(int score) {
        this.visitCount = score;
    }

    public void updateCurationLabel(CurationLabel label) {
        this.curationLabel = label;
    }
}
