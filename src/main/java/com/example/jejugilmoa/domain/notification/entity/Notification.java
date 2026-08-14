package com.example.jejugilmoa.domain.notification.entity;

import com.example.jejugilmoa.domain.notification.enums.NotificationType;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "notification",
        indexes = {
                @Index(name = "idx_notification_user_created", columnList = "user_id,created_at")
        }
)
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Notification extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private NotificationType type;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String body;

    // 딥링크 등 클라이언트 처리용 JSON 페이로드
    @Column(columnDefinition = "TEXT")
    private String data;
}
