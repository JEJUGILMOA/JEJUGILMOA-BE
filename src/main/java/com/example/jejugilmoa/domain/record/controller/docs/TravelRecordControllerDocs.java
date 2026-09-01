package com.example.jejugilmoa.domain.record.controller.docs;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCreateRequest;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCreateResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordDetailResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordReactionRequest;
import com.example.jejugilmoa.domain.record.dto.TravelRecordUpdateRequest;
import com.example.jejugilmoa.domain.record.dto.TravelRecordUpdateResponse;
import com.example.jejugilmoa.domain.record.enums.RecordView;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public interface TravelRecordControllerDocs {

    @Operation(
            summary = "여행 기록 목록 조회",
            description = "CARD 또는 MAP 형태로 내 기록이나 공개 기록을 페이지 조회합니다."
    )
    ApiResponse<PageResponse<?>> getRecords(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "CARD") RecordView view,
            @RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size);

    @Operation(
            summary = "여행 기록 상세 조회",
            description = "본인 기록은 공개 범위와 무관하게, 타인 기록은 PUBLIC인 경우에만 조회합니다."
    )
    ApiResponse<TravelRecordDetailResponse> getDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long recordId);

    @Operation(
            summary = "여행 기록 반응 설정",
            description = "타인의 PUBLIC 기록에 대한 현재 반응을 LIKE 또는 DISLIKE로 멱등 설정합니다."
    )
    ApiResponse<Void> setReaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long recordId,
            @Valid @org.springframework.web.bind.annotation.RequestBody TravelRecordReactionRequest request);

    @Operation(
            summary = "여행 기록 반응 취소",
            description = "타인의 PUBLIC 기록에 대한 현재 반응을 멱등 취소합니다."
    )
    ApiResponse<Void> deleteReaction(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long recordId);

    @Operation(
            summary = "여행 기록 생성",
            description = """
                    로그인 사용자가 소유한 완료(COMPLETED) 여행으로 기록을 생성합니다.

                    - tripId는 여행 계획 ID와 같습니다.
                    - 장소는 서버가 완료 여행의 전체 경유지를 날짜/순서대로 복사합니다.
                    - 장소 메모와 선택 이미지들은 travelCourseId로 지정하며, 다른 여행의 경유지는 거부합니다.
                    - visibility를 생략하면 PRIVATE입니다.
                    - imageObjectKeys는 먼저 `/api/image-uploads`에서 발급받은
                      `records/{userId}/...` 형식이어야 하며 요청 순서대로 저장됩니다.
                    - 하나의 여행에는 하나의 기록만 생성할 수 있습니다.
                    """
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = TravelRecordCreateRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "tripId": 1,
                              "title": "제주에서 보낸 여름",
                              "description": "바다와 오름을 함께 본 하루",
                              "visibility": "PUBLIC",
                              "placeMemos": [
                                {
                                  "travelCourseId": 7,
                                  "memo": "노을 시간에 다시 오고 싶은 곳",
                                  "imageObjectKeys": ["records/42/place1.jpg", "records/42/place2.jpg"]
                                }
                              ],
                              "imageObjectKeys": [
                                "records/42/550e8400-e29b-41d4-a716-446655440000.jpg"
                              ]
                            }
                            """)
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201", description = "기록 생성 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {
                              "isSuccess": true,
                              "code": "COMMON201",
                              "message": "성공적으로 응답이 생성되었습니다.",
                              "result": {
                                "recordId": 10,
                                "tripId": 1,
                                "title": "제주에서 보낸 여름",
                                "visibility": "PUBLIC",
                                "createdAt": "2026-08-12T03:00:00Z"
                              }
                            }
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400", description = "미완료 여행, 메모 대상 불일치 또는 잘못된 이미지 키"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "다른 사용자의 여행"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "여행 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "여행 기록 중복",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"RECORD409_1","message":"이미 해당 여행의 기록이 존재합니다.","result":null}
                            """)))
    })
    ApiResponse<TravelRecordCreateResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @org.springframework.web.bind.annotation.RequestBody TravelRecordCreateRequest request);

    @Operation(
            summary = "여행 기록 수정",
            description = """
                    본인의 활성 여행 기록에서 제목, 소개, 공개 범위, 장소 메모와 사진만 수정합니다.
                    places의 recordPlaceId는 현재 기록의 snapshot 장소 ID입니다.
                    장소 image를 생략하면 유지하고, action은 REPLACE 또는 REMOVE를 사용합니다.
                    REPLACE의 objectKeys 배열 순서대로 장소 사진 전체를 교체합니다.
                    imageObjectKeys는 null이면 유지, 빈 배열이면 전체 기록 사진을 제거합니다.
                    """
    )
    ApiResponse<TravelRecordUpdateResponse> update(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long recordId,
            @Valid @org.springframework.web.bind.annotation.RequestBody TravelRecordUpdateRequest request);

    @Operation(summary = "여행 기록 삭제", description = "본인의 활성 여행 기록을 soft delete합니다.")
    ApiResponse<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long recordId);
}
