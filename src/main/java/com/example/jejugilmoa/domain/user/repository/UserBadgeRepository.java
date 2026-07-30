package com.example.jejugilmoa.domain.user.repository;

import com.example.jejugilmoa.domain.user.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findAllByUserId(Long userId);
}
