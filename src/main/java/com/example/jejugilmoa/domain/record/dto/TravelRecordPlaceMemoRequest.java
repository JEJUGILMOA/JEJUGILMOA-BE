package com.example.jejugilmoa.domain.record.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TravelRecordPlaceMemoRequest(
        @NotNull(message = "메모 대상 경유지 ID는 필수입니다.") Long travelCourseId,
        @Size(max = 1000, message = "장소 메모는 1000자를 초과할 수 없습니다.") String memo,
        List<@NotBlank(message = "이미지 objectKey는 비어 있을 수 없습니다.")
                @Size(max = 500, message = "이미지 objectKey는 500자를 초과할 수 없습니다.") String> imageObjectKeys
) {
}
