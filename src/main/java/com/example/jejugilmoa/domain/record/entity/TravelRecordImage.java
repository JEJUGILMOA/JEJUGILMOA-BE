package com.example.jejugilmoa.domain.record.entity;

import com.example.jejugilmoa.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;


@Entity
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(
        name = "travel_record_image",
        indexes = {
                @Index(name = "idx_image_record", columnList = "travel_record_id"),
                @Index(name = "idx_image_record_place", columnList = "travel_record_place_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_record_image_sequence",
                        columnNames = {"travel_record_id", "sequence_order"}
                )
        }
)
public class TravelRecordImage extends BaseEntity {

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
            name = "travel_record_place_id",
            nullable = true
    )
    private TravelRecordPlace travelRecordPlace;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(nullable = false)
    private int sequenceOrder;
}
