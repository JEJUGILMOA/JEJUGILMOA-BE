package com.example.jejugilmoa.domain.imageupload.service;

import com.example.jejugilmoa.domain.imageupload.exception.ImageUploadErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.example.jejugilmoa.global.external.s3.AwsProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.core.exception.SdkException;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.S3Exception;

@Component
@Profile("!local")
@RequiredArgsConstructor
@Slf4j
public class S3ImageObjectVerifier implements ImageObjectVerifier {

    private final S3Client s3Client;
    private final AwsProperties awsProperties;
    private final ImageObjectPolicy imageObjectPolicy;

    @Override
    public void verify(String objectKey) {
        try {
            var response = s3Client.headObject(HeadObjectRequest.builder()
                    .bucket(awsProperties.s3().bucket())
                    .key(objectKey)
                    .build());
            imageObjectPolicy.validateAndGetExtension(response.contentType(), response.contentLength());
        } catch (NoSuchKeyException exception) {
            throw new GeneralException(ImageUploadErrorCode.OBJECT_NOT_FOUND);
        } catch (S3Exception exception) {
            if (exception.statusCode() == 404) {
                throw new GeneralException(ImageUploadErrorCode.OBJECT_NOT_FOUND);
            }
            log.error("S3 이미지 객체 검증 실패. bucket={}, objectKey={}, status={}",
                    awsProperties.s3().bucket(), objectKey, exception.statusCode(), exception);
            throw new GeneralException(ImageUploadErrorCode.OBJECT_VERIFICATION_FAILED);
        } catch (SdkException exception) {
            log.error("S3 이미지 객체 검증 호출 실패. bucket={}, objectKey={}",
                    awsProperties.s3().bucket(), objectKey, exception);
            throw new GeneralException(ImageUploadErrorCode.OBJECT_VERIFICATION_FAILED);
        }
    }
}
