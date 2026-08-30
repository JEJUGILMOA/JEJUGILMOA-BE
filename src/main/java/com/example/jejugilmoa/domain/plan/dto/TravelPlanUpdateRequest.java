package com.example.jejugilmoa.domain.plan.dto;

import com.example.jejugilmoa.domain.plan.enums.TravelTheme;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.validator.constraints.UniqueElements;

import java.util.List;

public record TravelPlanUpdateRequest(

        @Schema(description = "여행 제목 (null이면 수정 안 함)", example = "제주 가을 여행")
        @Size(max = 200)
        String title,

        @Schema(description = "선호 테마 목록 (null이면 수정 안 함, 전달 시 1개 이상, 중복 불가)", example = "[\"NATURE\", \"CAFE\"]")
        @Size(min = 1)
        @UniqueElements
        List<@NotNull TravelTheme> categories

) {}
