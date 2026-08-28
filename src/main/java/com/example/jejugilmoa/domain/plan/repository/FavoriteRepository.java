package com.example.jejugilmoa.domain.plan.repository;

import com.example.jejugilmoa.domain.plan.entity.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    long countByUserId(Long userId);

    boolean existsByUserIdAndPlaceId(Long userId, Long placeId);

    Optional<Favorite> findByUserIdAndPlaceId(Long userId, Long placeId);

    @EntityGraph(attributePaths = {"place", "place.category"})
    Page<Favorite> findAllByUserIdAndUserDeletedAtIsNullAndPlacePublishedTrueOrderByIdDesc(
            Long userId,
            Pageable pageable
    );
}
