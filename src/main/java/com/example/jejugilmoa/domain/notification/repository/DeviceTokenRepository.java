package com.example.jejugilmoa.domain.notification.repository;

import com.example.jejugilmoa.domain.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {
    Optional<DeviceToken> findByUserIdAndDeviceId(Long userId, String deviceId);
    List<DeviceToken> findAllByUserId(Long userId);
}
