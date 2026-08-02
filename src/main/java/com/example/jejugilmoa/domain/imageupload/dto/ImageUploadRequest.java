package com.example.jejugilmoa.domain.imageupload.dto;

import jakarta.validation.constraints.NotNull;

public record ImageUploadRequest(
        @NotNull(message = "콘텐츠 타입은 필수입니다.") String contentType,
        @NotNull(message = "파일 크기는 필수입니다.") Long fileSize
) {
}
