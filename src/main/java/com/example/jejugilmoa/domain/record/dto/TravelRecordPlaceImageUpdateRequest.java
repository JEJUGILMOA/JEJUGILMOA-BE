package com.example.jejugilmoa.domain.record.dto;

import com.example.jejugilmoa.domain.record.enums.RecordPlaceImageAction;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TravelRecordPlaceImageUpdateRequest(
        @NotNull(message = "장소 이미지 변경 동작은 필수입니다.") RecordPlaceImageAction action,
        List<@NotBlank(message = "이미지 objectKey는 비어 있을 수 없습니다.")
                @Size(max = 500, message = "이미지 objectKey는 500자를 초과할 수 없습니다.") String> objectKeys
) {
}
