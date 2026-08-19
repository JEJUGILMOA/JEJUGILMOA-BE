package com.example.jejugilmoa.domain.imageupload.service;

import com.example.jejugilmoa.domain.imageupload.exception.ImageUploadErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.example.jejugilmoa.global.external.s3.AwsProperties;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ImageObjectPolicy {

    private static final Map<String, String> EXTENSIONS = Map.of(
            "image/jpeg", "jpg",
            "image/png", "png",
            "image/webp", "webp"
    );

    private final AwsProperties awsProperties;

    public String validateAndGetExtension(String contentType, long fileSize) {
        String extension = EXTENSIONS.get(contentType);
        if (extension == null) {
            throw new GeneralException(ImageUploadErrorCode.UNSUPPORTED_CONTENT_TYPE);
        }
        if (fileSize <= 0 || fileSize > awsProperties.s3().maxImageSizeBytes()) {
            throw new GeneralException(ImageUploadErrorCode.INVALID_FILE_SIZE);
        }
        return extension;
    }
}
