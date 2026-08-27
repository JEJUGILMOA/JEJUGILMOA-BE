package com.example.jejugilmoa.domain.record.entity;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "travel_record_place", indexes = {
        @Index(name = "idx_travel_record_place_record", columnList = "travel_record_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_record_place_sequence", columnNames = {"travel_record_id", "sequence_order"})
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TravelRecordPlace extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "travel_record_id",
            nullable = false
    )
    private TravelRecord travelRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "travel_place_id",
            nullable = false
    )
    private Place place;

    @Column(nullable = false, length = 200)
    private String placeName;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @Column(nullable = false)
    private LocalDate visitDate;

    @Column(nullable = false)
    private int sequenceOrder;  // 방문 순서

    @Column(length = 1000)
    private String memo;  // 감상/팁/메모

    @Builder.Default
    @Column(nullable = false)
    private boolean visited = true; // 방문 여부

    @Column
    private LocalDateTime visitedAt;      // 실제 방문 시각

    @Column
    private Integer stayMinutes;          // 실제 머문 시간

    @Column
    private Integer rating;            // 이 장소에 대한 개인 평점

    public void updateMemo(String memo) {
        this.memo = memo;
    }
}
