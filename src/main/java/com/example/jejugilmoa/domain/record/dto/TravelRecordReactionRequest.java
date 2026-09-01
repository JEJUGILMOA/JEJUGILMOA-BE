package com.example.jejugilmoa.domain.record.dto;

import com.example.jejugilmoa.domain.record.enums.ReactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

public record TravelRecordReactionRequest(
        @Schema(description = "설정할 기록 반응", example = "LIKE")
        @NotNull(message = "반응 타입은 필수입니다.")
        ReactionType reactionType
) {
}
