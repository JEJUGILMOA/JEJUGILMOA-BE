package com.example.jejugilmoa.domain.record.controller.docs;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCreateRequest;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCreateResponse;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

public interface TravelRecordControllerDocs {

    @Operation(
            summary = "여행 기록 생성",
            description = """
                    로그인 사용자가 소유한 완료(COMPLETED) 여행으로 기록을 생성합니다.

                    - tripId는 여행 계획 ID와 같습니다.
                    - 장소는 서버가 완료 여행의 전체 경유지를 날짜/순서대로 복사합니다.
                    - 장소 메모와 선택 이미지 1장은 travelCourseId로 지정하며, 다른 여행의 경유지는 거부합니다.
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
                                {"travelCourseId": 7, "memo": "노을 시간에 다시 오고 싶은 곳", "imageObjectKey": "records/42/place.jpg"}
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
}
