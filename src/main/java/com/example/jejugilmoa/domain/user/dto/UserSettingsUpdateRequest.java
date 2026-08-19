package com.example.jejugilmoa.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record UserSettingsUpdateRequest(

        @Schema(description = "여행 시작 알림 수신 여부 (null이면 수정 안 함)", example = "true")
        Boolean notifyPlanStart,

        @Schema(description = "여행 기록 작성 알림 수신 여부 (null이면 수정 안 함)", example = "true")
        Boolean notifyRecordWriting,

        @Schema(description = "배지 획득 알림 수신 여부 (null이면 수정 안 함)", example = "true")
        Boolean notifyBadgeAcquired,

        @Schema(description = "다음 장소 알림 수신 여부 (null이면 수정 안 함)", example = "true")
        Boolean notifyNextPlace,

        @Schema(description = "장소 도착 알림 수신 여부 (null이면 수정 안 함)", example = "true")
        Boolean notifyPlaceArrival,

        @Schema(description = "마케팅 알림 수신 여부 (null이면 수정 안 함)", example = "false")
        Boolean notifyMarketing,

        @Schema(description = "위치 권한 허용 여부 (null이면 수정 안 함)", example = "true")
        Boolean locationPermission

) {}
