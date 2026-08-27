package com.example.jejugilmoa.domain.record.dto;

import com.example.jejugilmoa.domain.record.enums.RecordPlaceImageAction;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record TravelRecordPlaceImageUpdateRequest(
        @NotNull(message = "장소 이미지 변경 동작은 필수입니다.") RecordPlaceImageAction action,
        @Size(max = 500, message = "이미지 objectKey는 500자를 초과할 수 없습니다.") String objectKey
) {
}
