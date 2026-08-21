package com.example.jejugilmoa.domain.notification.service;

import com.example.jejugilmoa.domain.notification.converter.NotificationConverter;
import com.example.jejugilmoa.domain.notification.dto.NotificationReadRequest;
import com.example.jejugilmoa.domain.notification.dto.NotificationResponse;
import com.example.jejugilmoa.domain.notification.dto.UnreadCountResponse;
import com.example.jejugilmoa.domain.notification.entity.Notification;
import com.example.jejugilmoa.domain.notification.exception.NotificationErrorCode;
import com.example.jejugilmoa.domain.notification.repository.NotificationReadRepository;
import com.example.jejugilmoa.domain.notification.repository.NotificationRepository;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationQueryService {

    private final NotificationRepository notificationRepository;
    private final NotificationReadRepository notificationReadRepository;
    private final NotificationConverter notificationConverter;

    public PageResponse<NotificationResponse> list(Long userId, Pageable pageable) {
        Page<Notification> notifications = notificationRepository
                .findAllByUserIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(userId, pageable);

        List<Long> notificationIds = notifications.getContent().stream()
                .map(Notification::getId)
                .toList();
        Set<Long> readIds = notificationIds.isEmpty()
                ? Set.of()
                : Set.copyOf(notificationReadRepository.findReadNotificationIds(notificationIds, userId));

        Page<NotificationResponse> responses = notifications.map(
                notification -> notificationConverter.toResponse(notification, readIds.contains(notification.getId())));
        return PageResponse.of(responses);
    }

    public UnreadCountResponse unreadCount(Long userId) {
        return new UnreadCountResponse(notificationRepository.countUnreadByUserId(userId));
    }

    @Transactional
    public void markAsRead(Long userId, NotificationReadRequest request) {
        if (Boolean.TRUE.equals(request.all())) {
            notificationReadRepository.markAllAsRead(userId);
            return;
        }
        if (request.notificationIds() == null || request.notificationIds().isEmpty()) {
            throw new GeneralException(NotificationErrorCode.INVALID_READ_REQUEST);
        }
        notificationReadRepository.markAsRead(request.notificationIds(), userId);
    }

    @Transactional
    public void delete(Long userId, Long notificationId) {
        Notification notification = notificationRepository
                .findByIdAndUserIdAndDeletedAtIsNull(notificationId, userId)
                .orElseThrow(() -> new GeneralException(NotificationErrorCode.NOTIFICATION_NOT_FOUND));
        notification.delete();
    }
}
