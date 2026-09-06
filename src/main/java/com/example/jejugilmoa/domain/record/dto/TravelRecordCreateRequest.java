package com.example.jejugilmoa.domain.record.dto;

import com.example.jejugilmoa.domain.plan.enums.Visibility;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record TravelRecordCreateRequest(
        @NotNull(message = "여행 ID는 필수입니다.") Long tripId,
        @NotBlank(message = "기록 제목은 필수입니다.")
        @Size(max = 50, message = "기록 제목은 50자를 초과할 수 없습니다.") String title,
        String description,
        Visibility visibility,
        List<@Valid TravelRecordPlaceMemoRequest> placeMemos,
        List<@NotBlank(message = "이미지 objectKey는 비어 있을 수 없습니다.")
                @Size(max = 500, message = "이미지 objectKey는 500자를 초과할 수 없습니다.") String> imageObjectKeys,
        @Pattern(regexp = ".*\\S.*", message = "썸네일 이미지 objectKey는 비어 있을 수 없습니다.")
        @Size(max = 500, message = "썸네일 이미지 objectKey는 500자를 초과할 수 없습니다.")
        @Schema(description = "생성 요청의 최상위 또는 장소 이미지 중 썸네일로 사용할 objectKey입니다. "
                + "생략하거나 null이면 sequenceOrder가 가장 작은 이미지를 선택하며, 이미지가 없으면 썸네일도 없습니다. "
                + "요청에 없는 키는 RECORD400_7 오류이며, 썸네일 선택은 이미지 표시 순서에 영향을 주지 않습니다.",
                example = "records/42/place-1.jpg")
        String thumbnailImageObjectKey
) {
    public TravelRecordCreateRequest(Long tripId, String title, String description, Visibility visibility,
                                     List<TravelRecordPlaceMemoRequest> placeMemos, List<String> imageObjectKeys) {
        this(tripId, title, description, visibility, placeMemos, imageObjectKeys, null);
    }
}
