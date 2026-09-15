package com.example.jejugilmoa.domain.record.controller.docs;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCardResponse;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

public interface TravelRecordFavoriteControllerDocs {
    @Operation(summary = "기록 즐겨찾기 생성", description = "활성 사용자가 차단 관계가 없는 타인의 PUBLIC 기록을 저장합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "저장 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "자기 기록 저장 불가"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "접근 가능한 기록 없음"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "이미 저장한 기록")
    })
    ApiResponse<Void> add(UserPrincipal principal, Long recordId);

    @Operation(summary = "기록 즐겨찾기 취소", description = "본인의 저장 관계만 물리 삭제합니다. 기록 상태와 무관하며 이미 없어도 200을 반환합니다.")
    ApiResponse<Void> delete(UserPrincipal principal, Long recordId);

    @Operation(summary = "내 기록 즐겨찾기 조회", description = "접근 가능한 PUBLIC 기록을 저장 시각 내림차순, 동일 시각에는 즐겨찾기 ID 내림차순으로 조회합니다. page는 0 이상, size는 1~100(기본 20)입니다. 카드만 반환하며 MAP 조회는 지원하지 않습니다.")
    ApiResponse<PageResponse<TravelRecordCardResponse>> list(UserPrincipal principal, int page, int size);
}
