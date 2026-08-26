package com.example.jejugilmoa.domain.record.dto;

import com.example.jejugilmoa.domain.plan.enums.Visibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TravelRecordUpdateRequest(
        @Size(max = 50, message = "기록 제목은 50자를 초과할 수 없습니다.")
        @Pattern(regexp = ".*\\S.*", message = "기록 제목은 비어 있을 수 없습니다.") String title,
        String description,
        Visibility visibility,
        List<@Valid TravelRecordPlaceUpdateRequest> places,
        List<@jakarta.validation.constraints.NotBlank(message = "이미지 objectKey는 비어 있을 수 없습니다.")
                @Size(max = 500, message = "이미지 objectKey는 500자를 초과할 수 없습니다.") String> imageObjectKeys
) {
}
