package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

public record WaypointMemoRequest(

        @Size(max = 1000, message = "메모는 1000자 이내로 입력해주세요.")
        @Schema(description = "장소 메모 (null 또는 빈 문자열로 전송 시 메모 삭제)", example = "노을 시간대 방문 추천, 파라솔 대여 5천원")
        String memo

) {}
