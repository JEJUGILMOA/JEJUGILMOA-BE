package com.example.jejugilmoa.domain.user.repository;

import com.example.jejugilmoa.domain.user.entity.NotificationSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface NotificationSettingRepository extends JpaRepository<NotificationSetting, Long> {
    Optional<NotificationSetting> findByUserIdAndUserDeletedAtIsNull(Long userId);
}
