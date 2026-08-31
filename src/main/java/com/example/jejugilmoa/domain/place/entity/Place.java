package com.example.jejugilmoa.domain.place.entity;

import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Point;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;


@Entity
@Table(name = "place", indexes = {
        @Index(name = "idx_place_category", columnList = "category_id"),
        @Index(name = "idx_place_is_published", columnList = "is_published"),
        @Index(name = "idx_place_created_at", columnList = "created_at")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Place extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "external_id", length = 50, unique = true)
    private String externalId; // TourAPI contentId

    @Column(length = 200, nullable = false)
    private String name;

    @Column(length = 500, nullable = false)
    private String address;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;  // 위도

    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;  // 경도

    /**
     * PostGIS 지리 데이터 (GEOMETRY(Point, 4326))
     * ST_Point(longitude, latitude) 형태로 저장
     * 반경 검색에 사용: ST_DWithin()
     */
    @JdbcTypeCode(SqlTypes.GEOMETRY)
    @Column(columnDefinition = "geometry(Point,4326)")
    private Point geom;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 500)
    private String imageUrl;  // 대표 이미지

    @OneToMany(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PlaceImage> images = new ArrayList<>();  // 갤러리 이미지

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "category_id",
            nullable = false
    )
    private Category category;

    @Builder.Default
    @Column(nullable = false)
    private int visitorCount = 0;  // 방문자 수

    @Builder.Default
    @Column(name = "is_published")
    private boolean published = true;  // 노출 여부

    @OneToMany(mappedBy = "place")
    @Builder.Default
    private List<TravelCourse> travelCourses = new ArrayList<>();

    public void updateCommonInfo(String overview) {
        if (overview != null) this.description = overview;
    }

    public void updateImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
}
