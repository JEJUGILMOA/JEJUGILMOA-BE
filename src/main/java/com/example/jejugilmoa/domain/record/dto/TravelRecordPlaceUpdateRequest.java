package com.example.jejugilmoa.domain.record.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TravelRecordPlaceUpdateRequest(
        @NotNull(message = "기록 장소 ID는 필수입니다.") Long recordPlaceId,
        @Size(max = 1000, message = "장소 메모는 1000자를 초과할 수 없습니다.") String memo,
        @Valid TravelRecordPlaceImageUpdateRequest image
) {
}
