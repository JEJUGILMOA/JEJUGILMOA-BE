package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record WaypointReorderRequest(

        @Schema(description = "순서를 변경할 날짜", example = "2026-08-15")
        @NotNull(message = "날짜는 필수입니다.")
        LocalDate visitDate,

        @NotEmpty(message = "경유지 ID 목록은 비어있을 수 없습니다.")
        @Schema(description = "해당 날짜의 경유지 ID를 원하는 순서로 (인덱스 0 = 1번 순서)",
                example = "[3, 1, 2]")
        List<Long> waypointIds

) {}
