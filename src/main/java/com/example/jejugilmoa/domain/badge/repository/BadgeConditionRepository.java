package com.example.jejugilmoa.domain.badge.repository;

import com.example.jejugilmoa.domain.badge.entity.BadgeCondition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BadgeConditionRepository extends JpaRepository<BadgeCondition, Long> {
    List<BadgeCondition> findAllByBadgeIdIn(List<Long> badgeIds);
}
