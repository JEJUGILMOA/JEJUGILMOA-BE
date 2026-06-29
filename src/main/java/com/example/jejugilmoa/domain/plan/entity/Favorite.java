package com.example.jejugilmoa.domain.plan.entity;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.user.entity.User;
import lombok.*;

import jakarta.persistence.*;

@Entity
@Table(name = "favorite", indexes = {
        @Index(name = "idx_favorite_user", columnList = "user_id"),
        @Index(name = "idx_favorite_place", columnList = "place_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_favorite", columnNames = {"user_id", "place_id"})
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)
    private Place place;

}
