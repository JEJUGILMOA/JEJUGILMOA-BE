package com.example.jejugilmoa.domain.notification.repository;

import com.example.jejugilmoa.domain.notification.entity.NotificationRead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface NotificationReadRepository extends JpaRepository<NotificationRead, Long> {

    @Query("SELECT nr.notification.id FROM NotificationRead nr WHERE nr.user.id = :userId AND nr.notification.id IN :notificationIds")
    List<Long> findReadNotificationIds(@Param("notificationIds") List<Long> notificationIds, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO notification_read (created_at, updated_at, notification_id, user_id, read_at)
            SELECT now(), now(), n.id, :userId, now()
            FROM notification n
            WHERE n.id IN (:notificationIds) AND n.user_id = :userId AND n.deleted_at IS NULL
            ON CONFLICT (notification_id, user_id) DO NOTHING
            """, nativeQuery = true)
    void markAsRead(@Param("notificationIds") List<Long> notificationIds, @Param("userId") Long userId);

    @Modifying(clearAutomatically = true)
    @Query(value = """
            INSERT INTO notification_read (created_at, updated_at, notification_id, user_id, read_at)
            SELECT now(), now(), n.id, :userId, now()
            FROM notification n
            WHERE n.user_id = :userId AND n.deleted_at IS NULL
            ON CONFLICT (notification_id, user_id) DO NOTHING
            """, nativeQuery = true)
    void markAllAsRead(@Param("userId") Long userId);
}
