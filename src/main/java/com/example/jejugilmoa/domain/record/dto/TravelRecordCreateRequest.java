package com.example.jejugilmoa.domain.record.dto;

import com.example.jejugilmoa.domain.plan.enums.Visibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TravelRecordCreateRequest(
        @NotNull(message = "여행 ID는 필수입니다.") Long tripId,
        @NotBlank(message = "기록 제목은 필수입니다.")
        @Size(max = 50, message = "기록 제목은 50자를 초과할 수 없습니다.") String title,
        String description,
        Visibility visibility,
        List<@Valid TravelRecordPlaceMemoRequest> placeMemos,
        List<@NotBlank(message = "이미지 objectKey는 비어 있을 수 없습니다.")
                @Size(max = 500, message = "이미지 objectKey는 500자를 초과할 수 없습니다.") String> imageObjectKeys
) {
}
