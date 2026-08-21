package com.example.jejugilmoa.domain.notification.repository;

import com.example.jejugilmoa.domain.notification.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Page<Notification> findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(Long userId, Pageable pageable);

    Optional<Notification> findByIdAndUserIdAndDeletedAtIsNull(Long id, Long userId);

    @Query("""
            SELECT COUNT(n) FROM Notification n
            WHERE n.user.id = :userId AND n.deletedAt IS NULL
              AND NOT EXISTS (
                  SELECT 1 FROM NotificationRead nr
                  WHERE nr.notification.id = n.id AND nr.user.id = :userId
              )
            """)
    long countUnreadByUserId(@Param("userId") Long userId);
}
