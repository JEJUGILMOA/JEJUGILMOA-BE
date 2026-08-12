package com.example.jejugilmoa.domain.record.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TravelRecordPlaceMemoRequest(
        @NotNull(message = "메모 대상 경유지 ID는 필수입니다.") Long travelCourseId,
        @Size(max = 5000, message = "장소 메모는 5000자를 초과할 수 없습니다.") String memo,
        @Size(max = 500, message = "이미지 objectKey는 500자를 초과할 수 없습니다.")
        @Pattern(regexp = ".*\\S.*", message = "이미지 objectKey는 비어 있을 수 없습니다.") String imageObjectKey
) {
}
