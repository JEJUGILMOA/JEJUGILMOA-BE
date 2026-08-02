package com.example.jejugilmoa.domain.imageupload.controller.docs;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.imageupload.dto.ImageUploadRequest;
import com.example.jejugilmoa.domain.imageupload.dto.ImageUploadResponse;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.RequestBody;

@Tag(name = "이미지 업로드", description = "S3 직접 이미지 업로드 API")
public interface ImageUploadControllerDocs {

    @Operation(
            summary = "이미지 업로드 URL 발급",
            description = """
                    로그인 사용자의 기록 이미지용 S3 Presigned PUT URL을 발급합니다.
                    응답의 Content-Type 헤더를 그대로 사용해 S3로 PUT해야 합니다.
                    fileSize는 사전 검증용 선언값이며, 실제 객체 크기와 타입은 추후 기록 생성 API가 HeadObject로 검증해야 합니다.
                    """
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "업로드 URL 발급 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "지원하지 않는 타입 또는 잘못된 파일 크기"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "업로드 URL 생성 실패")
    })
    ApiResponse<ImageUploadResponse> createUploadUrl(
            @Parameter(hidden = true) @AuthenticationPrincipal UserPrincipal principal,
            @RequestBody ImageUploadRequest request
    );
}
