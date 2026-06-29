package com.example.jejugilmoa.domain.place.entity;

import com.example.jejugilmoa.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "place_image",
        indexes = {
                @Index(name = "idx_place_image_place", columnList = "place_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_place_image_sequence",
                        columnNames = {"place_id", "sequence_order"}
                )
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PlaceImage extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    @Column(name = "sequence_order", nullable = false)
    private int sequenceOrder;
}
