package com.example.jejugilmoa.domain.notification.controller;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.notification.controller.docs.NotificationControllerDocs;
import com.example.jejugilmoa.domain.notification.dto.NotificationReadRequest;
import com.example.jejugilmoa.domain.notification.dto.NotificationResponse;
import com.example.jejugilmoa.domain.notification.dto.UnreadCountResponse;
import com.example.jejugilmoa.domain.notification.exception.NotificationErrorCode;
import com.example.jejugilmoa.domain.notification.service.NotificationQueryService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "알림", description = "알림 목록 조회 및 읽음 처리 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController implements NotificationControllerDocs {

    private static final int MAX_SIZE = 100;

    private final NotificationQueryService notificationQueryService;

    @Override
    @GetMapping
    public ApiResponse<PageResponse<NotificationResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0) throw new GeneralException(NotificationErrorCode.INVALID_PAGE);
        if (size < 1 || size > MAX_SIZE) throw new GeneralException(NotificationErrorCode.INVALID_PAGE_SIZE);
        return ApiResponse.onSuccess(GeneralSuccessCode.FOUND,
                notificationQueryService.list(principal.userId(), PageRequest.of(page, size)));
    }

    @Override
    @GetMapping("/unread-count")
    public ApiResponse<UnreadCountResponse> unreadCount(
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK,
                notificationQueryService.unreadCount(principal.userId()));
    }

    @Override
    @PatchMapping("/read")
    public ApiResponse<Void> markAsRead(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody NotificationReadRequest request) {
        notificationQueryService.markAsRead(principal.userId(), request);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }

    @Override
    @DeleteMapping("/{notificationId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long notificationId) {
        notificationQueryService.delete(principal.userId(), notificationId);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }
}
