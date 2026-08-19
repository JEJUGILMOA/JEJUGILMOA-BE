package com.example.jejugilmoa.domain.plan.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record BudgetCreateRequest(

        @Schema(description = "교통비 (null=미입력)", example = "50000")
        Integer budgetTransportation,

        @Schema(description = "숙박비 (null=미입력)", example = "150000")
        Integer budgetAccommodation,

        @Schema(description = "식비 (null=미입력)", example = "80000")
        Integer budgetFood,

        @Schema(description = "기타 (null=미입력)", example = "30000")
        Integer budgetEtc

) {}
