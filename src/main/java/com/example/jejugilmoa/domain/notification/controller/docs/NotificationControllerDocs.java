package com.example.jejugilmoa.domain.notification.controller.docs;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.notification.dto.NotificationReadRequest;
import com.example.jejugilmoa.domain.notification.dto.NotificationResponse;
import com.example.jejugilmoa.domain.notification.dto.UnreadCountResponse;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

public interface NotificationControllerDocs {

    @Operation(
        summary = "알림 목록 조회",
        description = """
            내 알림을 최신순으로 페이지네이션 조회합니다. 소프트 삭제된 알림은 제외됩니다.

            무한 스크롤: 응답의 `last`가 `false`이면 `page`를 1 증가시켜 다음 페이지를 요청합니다.
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                examples = @ExampleObject(value = """
                    {
                      "isSuccess": true,
                      "code": "FOUND200",
                      "message": "조회에 성공했습니다.",
                      "result": {
                        "content": [
                          {
                            "id": 10,
                            "type": "PLAN",
                            "title": "다음 일정이 30분 뒤 시작해요",
                            "body": "성산일출봉 · 13:00 도착 예정",
                            "createdAt": "2026-08-21T05:41:00Z",
                            "isRead": false,
                            "deepLink": "jejugilmoa://plans/1"
                          }
                        ],
                        "page": 0,
                        "size": 20,
                        "totalElements": 1,
                        "totalPages": 1,
                        "last": true
                      }
                    }
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "잘못된 페이지 파라미터",
            content = @Content(
                examples = @ExampleObject(value = """
                    {"isSuccess":false,"code":"NOTIFICATION400_2","message":"page는 0 이상이어야 합니다.","result":null}
                    """)
            )
        )
    })
    ApiResponse<PageResponse<NotificationResponse>> list(
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,

        @Parameter(description = "페이지 번호 (0부터 시작)", example = "0")
        @RequestParam(defaultValue = "0") int page,

        @Parameter(description = "페이지 크기 (1~100)", example = "20")
        @RequestParam(defaultValue = "20") int size
    );

    @Operation(
        summary = "미확인 알림 개수 조회",
        description = "MY-01 상단 알림 뱃지에 표시할 미확인 알림 개수입니다. 개별/전체 읽음 처리 시점에만 감소합니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "조회 성공",
            content = @Content(
                examples = @ExampleObject(value = """
                    {"isSuccess":true,"code":"COMMON200","message":"성공적으로 요청을 처리했습니다.","result":{"count":3}}
                    """)
            )
        )
    })
    ApiResponse<UnreadCountResponse> unreadCount(
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal
    );

    @Operation(
        summary = "알림 읽음 처리",
        description = """
            알림을 읽음 처리합니다.

            - 개별 읽음: `notificationIds`에 대상 id 목록 전달
            - 전체 읽음: `all: true` 전달 (이때 `notificationIds`는 무시됩니다)

            이미 읽은 알림을 다시 요청해도 안전합니다(멱등).
            """
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "처리 성공",
            content = @Content(
                examples = @ExampleObject(value = """
                    {"isSuccess":true,"code":"COMMON200","message":"성공적으로 요청을 처리했습니다.","result":null}
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "400",
            description = "notificationIds와 all이 모두 비어있음",
            content = @Content(
                examples = @ExampleObject(value = """
                    {"isSuccess":false,"code":"NOTIFICATION400_1","message":"notificationIds 또는 all 중 하나는 필요합니다.","result":null}
                    """)
            )
        )
    })
    ApiResponse<Void> markAsRead(
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
        @RequestBody NotificationReadRequest request
    );

    @Operation(
        summary = "알림 삭제 (스와이프 삭제)",
        description = "알림을 소프트 삭제합니다. 삭제된 알림은 목록/미확인 개수에서 즉시 제외됩니다."
    )
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "200",
            description = "삭제 성공",
            content = @Content(
                examples = @ExampleObject(value = """
                    {"isSuccess":true,"code":"COMMON200","message":"성공적으로 요청을 처리했습니다.","result":null}
                    """)
            )
        ),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(
            responseCode = "404",
            description = "존재하지 않거나 내 알림이 아님",
            content = @Content(
                examples = @ExampleObject(value = """
                    {"isSuccess":false,"code":"NOTIFICATION404_1","message":"알림을 찾을 수 없습니다.","result":null}
                    """)
            )
        )
    })
    ApiResponse<Void> delete(
        @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
        @Parameter(description = "알림 ID", example = "10") @PathVariable Long notificationId
    );
}
