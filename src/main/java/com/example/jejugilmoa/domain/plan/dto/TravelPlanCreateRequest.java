package com.example.jejugilmoa.domain.plan.dto;

import com.example.jejugilmoa.domain.plan.enums.TravelCompanion;
import com.example.jejugilmoa.domain.plan.enums.TravelTheme;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;

import java.time.LocalDate;
import java.util.List;

public record TravelPlanCreateRequest(

        @Schema(description = "여행 제목", example = "제주 여름 휴가")
        @NotBlank @Size(max = 200)
        String title,

        @Schema(description = "여행 시작일", example = "2026-08-15")
        @NotNull
        LocalDate startDate,

        @Schema(description = "여행 종료일 (시작일과 같거나 이후)", example = "2026-08-17")
        @NotNull
        LocalDate endDate,

        @Schema(description = "동반자 유형", example = "COUPLE")
        TravelCompanion companion,

        @Schema(description = "선호 테마 목록 (생략 가능, 전달 시 1개 이상, 중복 불가)", example = "[\"NATURE\", \"CAFE\"]")
        @Size(min = 1)
        @UniqueElements
        List<@NotNull TravelTheme> categories,

        @Schema(description = "날짜별 경유지 목록 (계획 생성 시 초기 경유지, 빈 배열 허용). 각 day에 출발지 정보 포함.")
        @Valid
        List<DayPlanRequest> days,

        @Schema(description = "예산 (생략 또는 null 시 예산 없음으로 덮어씀)")
        BudgetCreateRequest budget

) {}
