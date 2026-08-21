package com.example.jejugilmoa.domain.notification.service;

import com.example.jejugilmoa.domain.notification.converter.NotificationConverter;
import com.example.jejugilmoa.domain.notification.dto.NotificationReadRequest;
import com.example.jejugilmoa.domain.notification.dto.NotificationResponse;
import com.example.jejugilmoa.domain.notification.entity.Notification;
import com.example.jejugilmoa.domain.notification.enums.NotificationCategory;
import com.example.jejugilmoa.domain.notification.enums.NotificationType;
import com.example.jejugilmoa.domain.notification.exception.NotificationErrorCode;
import com.example.jejugilmoa.domain.notification.repository.NotificationReadRepository;
import com.example.jejugilmoa.domain.notification.repository.NotificationRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificationQueryServiceTest {

    @Mock NotificationRepository notificationRepository;
    @Mock NotificationReadRepository notificationReadRepository;
    @Mock NotificationConverter notificationConverter;
    @InjectMocks NotificationQueryService notificationQueryService;

    @Test
    void list_marksItemsReadBasedOnNotificationReadRows() {
        User user = User.builder().id(1L).nickname("여행자").build();
        Notification notification = Notification.builder()
                .id(10L).user(user).type(NotificationType.BADGE_ACQUIRED).title("배지 획득").build();
        Page<Notification> page = new PageImpl<>(List.of(notification));
        given(notificationRepository.findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(eq(1L), any()))
                .willReturn(page);
        given(notificationReadRepository.findReadNotificationIds(eq(List.of(10L)), eq(1L)))
                .willReturn(List.of(10L));
        given(notificationConverter.toResponse(notification, true))
                .willReturn(new NotificationResponse(10L, NotificationCategory.BADGE, "배지 획득", null,
                        Instant.now(), true, null));

        var result = notificationQueryService.list(1L, PageRequest.of(0, 20));

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).isRead()).isTrue();
    }

    @Test
    void unreadCount_delegatesToRepository() {
        given(notificationRepository.countUnreadByUserId(1L)).willReturn(3L);

        var result = notificationQueryService.unreadCount(1L);

        assertThat(result.count()).isEqualTo(3L);
    }

    @Test
    void markAsRead_withAllTrue_marksAllAndSkipsIdBasedMarking() {
        notificationQueryService.markAsRead(1L, new NotificationReadRequest(null, true));

        verify(notificationReadRepository).markAllAsRead(1L);
        verify(notificationReadRepository, never()).markAsRead(anyList(), any());
    }

    @Test
    void markAsRead_withIds_marksOnlyThoseIds() {
        notificationQueryService.markAsRead(1L, new NotificationReadRequest(List.of(10L, 11L), null));

        verify(notificationReadRepository).markAsRead(List.of(10L, 11L), 1L);
    }

    @Test
    void markAsRead_withEmptyIdsAndNoAll_throwsInvalidReadRequest() {
        assertThatThrownBy(() -> notificationQueryService.markAsRead(1L, new NotificationReadRequest(List.of(), false)))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getCode())
                .isEqualTo(NotificationErrorCode.INVALID_READ_REQUEST);
    }

    @Test
    void delete_marksNotificationDeletedWhenOwnedByUser() {
        User user = User.builder().id(1L).nickname("여행자").build();
        Notification notification = Notification.builder()
                .id(10L).user(user).type(NotificationType.PLAN_START).title("일정 시작").build();
        given(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(10L, 1L))
                .willReturn(Optional.of(notification));

        notificationQueryService.delete(1L, 10L);

        assertThat(notification.getDeletedAt()).isNotNull();
    }

    @Test
    void delete_throwsWhenNotificationNotOwnedOrMissing() {
        given(notificationRepository.findByIdAndUserIdAndDeletedAtIsNull(999L, 1L))
                .willReturn(Optional.empty());

        assertThatThrownBy(() -> notificationQueryService.delete(1L, 999L))
                .isInstanceOf(GeneralException.class)
                .extracting(e -> ((GeneralException) e).getCode())
                .isEqualTo(NotificationErrorCode.NOTIFICATION_NOT_FOUND);
    }
}
