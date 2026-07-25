package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RecommendationSkipRequest(

        @Schema(description = "건너뛸(제외할) 장소 ID 목록 (1개 이상)", example = "[10, 23]")
        @NotNull(message = "건너뛸 장소 ID 목록은 필수입니다.")
        @Size(min = 1, message = "건너뛸 장소를 1개 이상 지정해주세요.")
        List<Long> excludedPlaceIds

) {}
