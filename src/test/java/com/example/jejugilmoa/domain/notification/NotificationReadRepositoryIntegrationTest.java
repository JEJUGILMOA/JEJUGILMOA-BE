package com.example.jejugilmoa.domain.notification;

import com.example.jejugilmoa.domain.notification.entity.Notification;
import com.example.jejugilmoa.domain.notification.enums.NotificationType;
import com.example.jejugilmoa.domain.notification.repository.NotificationReadRepository;
import com.example.jejugilmoa.domain.notification.repository.NotificationRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class NotificationReadRepositoryIntegrationTest {

    @Autowired UserRepository userRepository;
    @Autowired NotificationRepository notificationRepository;
    @Autowired NotificationReadRepository notificationReadRepository;

    @Test
    void markAsRead_marksOnlyGivenIdsAndIsIdempotent() {
        User user = saveUser("알림 수신자");
        Long n1 = saveNotification(user);
        Long n2 = saveNotification(user);

        notificationReadRepository.markAsRead(java.util.List.of(n1), user.getId());
        notificationReadRepository.markAsRead(java.util.List.of(n1), user.getId());

        assertThat(notificationReadRepository.findReadNotificationIds(java.util.List.of(n1, n2), user.getId()))
                .containsExactly(n1);
    }

    @Test
    void markAllAsRead_marksEveryUnreadNotificationForUser() {
        User user = saveUser("알림 수신자");
        Long n1 = saveNotification(user);
        Long n2 = saveNotification(user);

        notificationReadRepository.markAllAsRead(user.getId());

        assertThat(notificationRepository.countUnreadByUserId(user.getId())).isZero();
        assertThat(notificationReadRepository.findReadNotificationIds(java.util.List.of(n1, n2), user.getId()))
                .containsExactlyInAnyOrder(n1, n2);
    }

    @Test
    void markAsRead_doesNotMarkAnotherUsersNotification() {
        User owner = saveUser("알림 소유자");
        User another = saveUser("다른 사용자");
        Long notificationId = saveNotification(owner);

        notificationReadRepository.markAsRead(java.util.List.of(notificationId), another.getId());

        assertThat(notificationReadRepository.findReadNotificationIds(java.util.List.of(notificationId), another.getId()))
                .isEmpty();
        assertThat(notificationRepository.countUnreadByUserId(owner.getId())).isEqualTo(1);
    }

    @Test
    void countUnreadByUserId_excludesSoftDeletedNotifications() {
        User user = saveUser("알림 수신자");
        Notification notification = notificationRepository.saveAndFlush(Notification.builder()
                .user(user).type(NotificationType.PLAN_START).title("테스트 알림").build());

        assertThat(notificationRepository.countUnreadByUserId(user.getId())).isEqualTo(1);

        notification.delete();
        notificationRepository.saveAndFlush(notification);

        assertThat(notificationRepository.countUnreadByUserId(user.getId())).isZero();
    }

    private User saveUser(String nickname) {
        return userRepository.saveAndFlush(User.builder().nickname(nickname).build());
    }

    private Long saveNotification(User user) {
        return notificationRepository.saveAndFlush(Notification.builder()
                .user(user).type(NotificationType.PLAN_START).title("테스트 알림").build()).getId();
    }
}
