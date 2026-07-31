package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record BudgetUpdateResponse(

        @Schema(description = "여행 계획 ID", example = "1")
        Long planId,

        @Schema(description = "교통비", example = "50000")
        Integer budgetTransportation,

        @Schema(description = "숙박비", example = "150000")
        Integer budgetAccommodation,

        @Schema(description = "식비", example = "80000")
        Integer budgetFood,

        @Schema(description = "기타 비용", example = "30000")
        Integer budgetEtc,

        @Schema(description = "총 예산 (모든 항목 null이면 null)", example = "280000")
        Integer totalBudget

) {}
