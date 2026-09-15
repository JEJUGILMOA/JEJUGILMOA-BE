package com.example.jejugilmoa.domain.report.controller.docs;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.report.dto.ReportCreateRequest;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;

@Tag(name = "신고", description = "게시글 신고 API")
public interface ReportControllerDocs {

    @Operation(
            summary = "여행 기록 신고",
            description = """
                    공개(PUBLIC) 상태인 타인의 여행 기록을 신고합니다.

                    - 본인 기록은 신고할 수 없습니다.
                    - 같은 기록에 중복 신고는 불가합니다.
                    - 비공개 기록은 존재하지 않는 것으로 처리됩니다.
                    - 신고가 5건 누적되면 기록이 자동으로 비공개 전환되고 관리자 심사 대상이 됩니다.
                    """
    )
    @RequestBody(
            required = true,
            content = @Content(
                    mediaType = "application/json",
                    schema = @Schema(implementation = ReportCreateRequest.class),
                    examples = @ExampleObject(value = """
                            {
                              "reasonSummary": "욕설 및 혐오 발언이 포함되어 있습니다.",
                              "reasonDetail": "본문 세 번째 단락에 특정 집단을 비하하는 표현이 있습니다."
                            }
                            """)
            )
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200", description = "신고 접수 성공",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":true,"code":"COMMON201","message":"성공적으로 응답이 생성되었습니다.","result":null}
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403", description = "본인 기록 신고 시도",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"REPORT403_1","message":"본인이 작성한 게시글은 신고할 수 없습니다.","result":null}
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404", description = "기록 없음 또는 비공개 기록",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"RECORD404_1","message":"여행 기록을 찾을 수 없습니다.","result":null}
                            """))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409", description = "중복 신고",
                    content = @Content(examples = @ExampleObject(value = """
                            {"isSuccess":false,"code":"REPORT409_1","message":"이미 신고한 게시글입니다.","result":null}
                            """))
            )
    })
    ApiResponse<Void> reportRecord(
            @PathVariable Long recordId,
            @Valid @org.springframework.web.bind.annotation.RequestBody ReportCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    );
}
