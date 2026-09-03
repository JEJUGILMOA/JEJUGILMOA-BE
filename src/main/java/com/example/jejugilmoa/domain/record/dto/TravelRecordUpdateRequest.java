package com.example.jejugilmoa.domain.record.dto;

import com.example.jejugilmoa.domain.plan.enums.Visibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.List;

public record TravelRecordUpdateRequest(
        @Size(max = 50, message = "기록 제목은 50자를 초과할 수 없습니다.")
        @Pattern(regexp = ".*\\S.*", message = "기록 제목은 비어 있을 수 없습니다.") String title,
        String description,
        Visibility visibility,
        List<@NotNull @Valid TravelRecordPlaceUpdateRequest> places,
        List<@jakarta.validation.constraints.NotBlank(message = "이미지 objectKey는 비어 있을 수 없습니다.")
                @Size(max = 500, message = "이미지 objectKey는 500자를 초과할 수 없습니다.") String> imageObjectKeys,
        @Pattern(regexp = ".*\\S.*", message = "썸네일 이미지 objectKey는 비어 있을 수 없습니다.")
        @Size(max = 500, message = "썸네일 이미지 objectKey는 500자를 초과할 수 없습니다.")
        @io.swagger.v3.oas.annotations.media.Schema(description = """
                기록 수정 완료 후 해당 기록에 포함된 이미지 중 썸네일로 사용할 이미지 objectKey입니다.
                최상위 기록 이미지와 장소별 이미지 모두 선택할 수 있습니다.
                생략하면 기존 썸네일을 유지하며, 기존 썸네일 이미지가 삭제된 경우 남은 첫 이미지가 자동 선택됩니다.
                썸네일 선택은 이미지 표시 순서(sequenceOrder)에 영향을 주지 않습니다.
                """) String thumbnailImageObjectKey
) {

    public TravelRecordUpdateRequest(
            String title,
            String description,
            Visibility visibility,
            List<TravelRecordPlaceUpdateRequest> places,
            List<String> imageObjectKeys) {
        this(title, description, visibility, places, imageObjectKeys, null);
    }
}
