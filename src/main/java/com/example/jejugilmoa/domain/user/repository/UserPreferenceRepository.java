package com.example.jejugilmoa.domain.user.repository;

import com.example.jejugilmoa.domain.user.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {
    Optional<UserPreference> findByUserIdAndUserDeletedAtIsNull(Long userId);
}
