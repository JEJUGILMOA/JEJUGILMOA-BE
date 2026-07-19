package com.example.jejugilmoa.domain.plan.repository;

import com.example.jejugilmoa.domain.plan.entity.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    long countByUserId(Long userId);
}
