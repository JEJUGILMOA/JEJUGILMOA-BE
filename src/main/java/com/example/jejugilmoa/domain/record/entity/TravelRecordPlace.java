package com.example.jejugilmoa.domain.record.entity;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.global.entity.BaseEntity;
import lombok.*;

import jakarta.persistence.*;

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

    @Column(nullable = false)
    private int sequenceOrder;  // 방문 순서

    @Column(columnDefinition = "TEXT")
    private String memo;  // 감상/팁/메모

    @Builder.Default
    @Column(nullable = false)
    private boolean visited = true; // 방문 여부

    @Column
    private LocalDateTime visitedAt;      // 실제 방문 시각

    @Column(nullable = false)
    private int stayMinutes;          // 실제 머문 시간

    @Column(nullable = false)
    private int rating;            // 이 장소에 대한 개인 평점

}
