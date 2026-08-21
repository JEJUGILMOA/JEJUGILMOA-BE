package com.example.jejugilmoa.domain.place.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

public record PlaceSyncResponse(

        @Schema(description = "성공한 시군구 수")
        int succeededCount,

        @Schema(description = "실패한 시군구 수")
        int failedCount,

        @Schema(description = "시군구별 동기화 결과")
        List<SignguSyncResult> results

) {
    public record SignguSyncResult(
            @Schema(description = "시군구 코드", example = "11")
            String signguCd,

            @Schema(description = "동기화 성공 여부")
            boolean succeeded,

            @Schema(description = "실패 사유 (성공 시 null)", nullable = true)
            String errorMessage
    ) {}
}
