package com.example.jejugilmoa.domain.notification.repository;

import com.example.jejugilmoa.domain.notification.entity.DeviceToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DeviceTokenRepository extends JpaRepository<DeviceToken, Long> {

    @Query("SELECT dt FROM DeviceToken dt WHERE dt.user.id = :userId AND dt.deviceId = :deviceId AND dt.user.deletedAt IS NULL")
    Optional<DeviceToken> findByUserIdAndDeviceId(@Param("userId") Long userId, @Param("deviceId") String deviceId);

    @Query("SELECT dt FROM DeviceToken dt WHERE dt.user.id = :userId AND dt.user.deletedAt IS NULL")
    List<DeviceToken> findAllByUserId(@Param("userId") Long userId);

    @Query("SELECT dt FROM DeviceToken dt WHERE dt.token = :token AND dt.user.deletedAt IS NULL")
    Optional<DeviceToken> findByToken(@Param("token") String token);

    // 동일 FCM 토큰을 보유한 다른 소유자 레코드를 원자적으로 제거 (현재 userId+deviceId 쌍은 보존)
    @Modifying(clearAutomatically = true)
    @Query("DELETE FROM DeviceToken dt WHERE dt.token = :token AND NOT (dt.user.id = :userId AND dt.deviceId = :deviceId)")
    void evictTokenExcluding(@Param("token") String token, @Param("userId") Long userId, @Param("deviceId") String deviceId);
}
