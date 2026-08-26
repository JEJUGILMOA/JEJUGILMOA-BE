package com.example.jejugilmoa.domain.imageupload.service;

import com.example.jejugilmoa.domain.imageupload.exception.ImageUploadErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.example.jejugilmoa.global.external.s3.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class S3PresignedImageUrlResolver implements ImageUrlResolver {

    private final S3Presigner s3Presigner;
    private final AwsProperties awsProperties;

    @Override
    public String resolve(String objectKey) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(awsProperties.s3().bucket())
                    .key(objectKey)
                    .build();
            var presigned = s3Presigner.presignGetObject(GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofSeconds(awsProperties.s3().presignExpirationSeconds()))
                    .getObjectRequest(getObjectRequest)
                    .build());
            return presigned.url().toString();
        } catch (RuntimeException exception) {
            log.error("이미지 조회 URL 생성 실패. bucket={}, objectKey={}",
                    awsProperties.s3().bucket(), objectKey, exception);
            throw new GeneralException(ImageUploadErrorCode.READ_URL_PRESIGN_FAILED);
        }
    }
}
