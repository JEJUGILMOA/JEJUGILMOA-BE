package com.example.jejugilmoa.domain.place.entity;


import com.example.jejugilmoa.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


import java.time.LocalDateTime;

@Entity
@Table(name = "place_congestion", indexes = {
        @Index(name = "idx_congestion_place_datetime", columnList = "place_id,date_time")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_congestion_place_datetime", columnNames = {"place_id", "date_time"})
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlaceCongestion extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(nullable = false)
    private LocalDateTime dateTime;  // 시간

    @Column
    private Integer visitorCount;  // 방문자 수

    @Column(length = 20)
    private String congestionLevel;  // LOW, MEDIUM, HIGH

}
