package com.example.jejugilmoa.domain.user.repository;

import com.example.jejugilmoa.domain.user.entity.UserBadge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface UserBadgeRepository extends JpaRepository<UserBadge, Long> {
    List<UserBadge> findByUserIdAndUserDeletedAtIsNull(Long userId);

    // 여행 완료 시 "이번 여행에서 획득한 뱃지" 목록을 보여주기 위한 조회
    List<UserBadge> findByUserIdAndUserDeletedAtIsNullAndAcquiredAtGreaterThanEqualOrderByAcquiredAtAsc(
            Long userId, LocalDateTime since);
}
