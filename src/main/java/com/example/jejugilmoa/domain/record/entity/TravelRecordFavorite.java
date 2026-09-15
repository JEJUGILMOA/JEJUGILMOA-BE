package com.example.jejugilmoa.domain.record.entity;

import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "travel_record_favorite", indexes = {
        @Index(name = "idx_record_favorite_user_created_id", columnList = "user_id,created_at DESC,id DESC"),
        @Index(name = "idx_record_favorite_record", columnList = "travel_record_id")
}, uniqueConstraints = @UniqueConstraint(name = "uk_record_favorite", columnNames = {"user_id", "travel_record_id"}))
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TravelRecordFavorite extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "travel_record_id", nullable = false)
    private TravelRecord travelRecord;
}
