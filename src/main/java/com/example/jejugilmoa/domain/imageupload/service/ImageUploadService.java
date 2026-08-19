package com.example.jejugilmoa.domain.imageupload.service;

import com.example.jejugilmoa.domain.imageupload.dto.ImageUploadRequest;
import com.example.jejugilmoa.domain.imageupload.dto.ImageUploadResponse;
import com.example.jejugilmoa.domain.imageupload.exception.ImageUploadErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.example.jejugilmoa.global.external.s3.AwsProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class ImageUploadService {

    private final S3Presigner s3Presigner;
    private final AwsProperties awsProperties;
    private final Clock clock;
    private final ImageObjectPolicy imageObjectPolicy;

    public ImageUploadResponse createUploadUrl(ImageUploadRequest request, Long userId) {
        if (userId == null) {
            throw new GeneralException(ImageUploadErrorCode.AUTHENTICATED_USER_NOT_FOUND);
        }

        String extension = imageObjectPolicy.validateAndGetExtension(request.contentType(), request.fileSize());

        String objectKey = "records/%d/%s.%s".formatted(userId, UUID.randomUUID(), extension);
        Duration expiration = Duration.ofSeconds(awsProperties.s3().presignExpirationSeconds());
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(awsProperties.s3().bucket())
                .key(objectKey)
                .contentType(request.contentType())
                .contentLength(request.fileSize())
                .build();

        try {
            var presigned = s3Presigner.presignPutObject(PutObjectPresignRequest.builder()
                    .signatureDuration(expiration)
                    .putObjectRequest(putObjectRequest)
                    .build());

            // fileSize는 사전 검증용 클라이언트 선언값이다. 실제 S3 객체 크기는
            // 추후 기록 생성 API에서 HeadObject로 반드시 다시 검증해야 한다.
            Instant expiresAt = clock.instant().plus(expiration);
            return new ImageUploadResponse(
                    objectKey,
                    presigned.url().toString(),
                    "PUT",
                    Map.of("Content-Type", request.contentType()),
                    expiresAt
            );
        } catch (RuntimeException exception) {
            log.error(
                "Presigned URL 생성 실패. bucket={}, region={}",
                awsProperties.s3().bucket(),
                awsProperties.region(),
                exception
            );
            throw new GeneralException(ImageUploadErrorCode.PRESIGN_FAILED);
        }
    }
}
