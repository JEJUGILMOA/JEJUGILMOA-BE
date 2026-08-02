package com.example.jejugilmoa.domain.imageupload.controller;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.imageupload.controller.docs.ImageUploadControllerDocs;
import com.example.jejugilmoa.domain.imageupload.dto.ImageUploadRequest;
import com.example.jejugilmoa.domain.imageupload.dto.ImageUploadResponse;
import com.example.jejugilmoa.domain.imageupload.service.ImageUploadService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/image-uploads")
@RequiredArgsConstructor
public class ImageUploadController implements ImageUploadControllerDocs {

    private final ImageUploadService imageUploadService;

    @Override
    @PostMapping
    public ApiResponse<ImageUploadResponse> createUploadUrl(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ImageUploadRequest request
    ) {
        Long userId = principal == null ? null : principal.userId();
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                imageUploadService.createUploadUrl(request, userId)
        );
    }
}
