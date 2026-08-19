package com.example.jejugilmoa.domain.notification;

import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class NotificationReadOwnerConstraintIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired JdbcClient jdbcClient;

    @Test
    void notificationRead_savesWhenNotificationOwnerMatches() {
        User owner = saveUser("알림 소유자");
        Long notificationId = saveNotification(owner.getId());

        int inserted = saveNotificationRead(notificationId, owner.getId());

        assertThat(inserted).isOne();
    }

    @Test
    void notificationRead_failsAtDatabaseWhenNotificationOwnerDiffers() {
        User owner = saveUser("알림 소유자");
        User anotherUser = saveUser("다른 사용자");
        Long notificationId = saveNotification(owner.getId());

        assertThatThrownBy(() -> saveNotificationRead(notificationId, anotherUser.getId()))
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("fk_notification_read_notification_owner");
    }

    private User saveUser(String nickname) {
        return userRepository.saveAndFlush(User.builder().nickname(nickname).build());
    }

    private Long saveNotification(Long userId) {
        return jdbcClient.sql("""
                        INSERT INTO notification (created_at, updated_at, user_id, type, title)
                        VALUES (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :userId, 'PLAN_START', '테스트 알림')
                        RETURNING id
                        """)
                .param("userId", userId)
                .query(Long.class)
                .single();
    }

    private int saveNotificationRead(Long notificationId, Long userId) {
        return jdbcClient.sql("""
                        INSERT INTO notification_read
                            (created_at, updated_at, notification_id, user_id, read_at)
                        VALUES
                            (CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, :notificationId, :userId, CURRENT_TIMESTAMP)
                        """)
                .param("notificationId", notificationId)
                .param("userId", userId)
                .update();
    }
}
