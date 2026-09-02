package com.example.jejugilmoa.domain.badge.repository;

import com.example.jejugilmoa.domain.badge.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BadgeRepository extends JpaRepository<Badge, Long> {
    // 뱃지 시드(BadgeDataInitializer) 멱등 처리용
    Optional<Badge> findByName(String name);
}
