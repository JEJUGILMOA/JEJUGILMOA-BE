package com.example.jejugilmoa.domain.report.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportCreateRequest(
        @Schema(description = "신고 사유 요약 (직접 입력, 최대 200자)", example = "욕설 및 혐오 발언이 포함되어 있습니다.")
        @NotBlank @Size(max = 200) String reasonSummary,

        @Schema(description = "신고 사유 상세 (선택, 최대 500자)", example = "본문 세 번째 단락에 특정 집단을 비하하는 표현이 있습니다.")
        @Size(max = 500) String reasonDetail
) {}
