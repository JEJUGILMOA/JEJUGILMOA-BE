package com.example.jejugilmoa.domain.imageupload.exception;

import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ImageUploadErrorCode implements BaseCode {
    UNSUPPORTED_CONTENT_TYPE(HttpStatus.BAD_REQUEST, "IMAGE400_1", "지원하지 않는 이미지 형식입니다."),
    INVALID_FILE_SIZE(HttpStatus.BAD_REQUEST, "IMAGE400_2", "이미지 크기는 0보다 크고 허용된 최대 크기 이하여야 합니다."),
    AUTHENTICATED_USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "IMAGE401_1", "인증된 사용자 정보를 확인할 수 없습니다."),
    PRESIGN_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "IMAGE500_1", "이미지 업로드 URL을 생성하지 못했습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
