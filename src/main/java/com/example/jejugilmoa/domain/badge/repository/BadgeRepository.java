package com.example.jejugilmoa.domain.badge.repository;

import com.example.jejugilmoa.domain.badge.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BadgeRepository extends JpaRepository<Badge, Long> {
}
