package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record WaypointReorderRequest(

        @NotEmpty(message = "경유지 ID 목록은 비어있을 수 없습니다.")
        @Schema(description = "새로운 순서로 정렬된 경유지(TravelCourse) ID 목록 (인덱스 0 = 1번 순서)",
                example = "[3, 1, 2]")
        List<Long> waypointIds

) {}
