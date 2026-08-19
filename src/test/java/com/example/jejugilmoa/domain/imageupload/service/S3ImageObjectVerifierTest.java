package com.example.jejugilmoa.domain.imageupload.service;

import com.example.jejugilmoa.domain.imageupload.exception.ImageUploadErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.example.jejugilmoa.global.external.s3.AwsProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectResponse;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class S3ImageObjectVerifierTest {

    @Mock S3Client s3Client;
    private S3ImageObjectVerifier verifier;

    @BeforeEach
    void setUp() {
        var properties = new AwsProperties("ap-northeast-2",
                new AwsProperties.S3("test-bucket", 900, 10_485_760));
        verifier = new S3ImageObjectVerifier(s3Client, properties, new ImageObjectPolicy(properties));
    }

    @Test
    void verify_succeedsForExistingAllowedImage() {
        given(s3Client.headObject(any(HeadObjectRequest.class))).willReturn(HeadObjectResponse.builder()
                .contentType("image/jpeg").contentLength(1024L).build());

        verifier.verify("records/42/image.jpg");
    }

    @Test
    void verify_failsWhenObjectDoesNotExist() {
        given(s3Client.headObject(any(HeadObjectRequest.class))).willThrow(S3Exception.builder()
                .statusCode(404).message("not found").build());

        assertCode(ImageUploadErrorCode.OBJECT_NOT_FOUND);
    }

    @Test
    void verify_failsForUnsupportedContentType() {
        given(s3Client.headObject(any(HeadObjectRequest.class))).willReturn(HeadObjectResponse.builder()
                .contentType("image/gif").contentLength(1024L).build());

        assertCode(ImageUploadErrorCode.UNSUPPORTED_CONTENT_TYPE);
    }

    @Test
    void verify_failsWhenObjectExceedsMaximumSize() {
        given(s3Client.headObject(any(HeadObjectRequest.class))).willReturn(HeadObjectResponse.builder()
                .contentType("image/webp").contentLength(10_485_761L).build());

        assertCode(ImageUploadErrorCode.INVALID_FILE_SIZE);
    }

    @Test
    void verify_translatesS3FailureToObjectVerificationFailed() {
        given(s3Client.headObject(any(HeadObjectRequest.class))).willThrow(S3Exception.builder()
                .statusCode(503).message("service unavailable").build());

        assertCode(ImageUploadErrorCode.OBJECT_VERIFICATION_FAILED);
    }

    private void assertCode(ImageUploadErrorCode expectedCode) {
        assertThatThrownBy(() -> verifier.verify("records/42/image.jpg"))
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> org.assertj.core.api.Assertions.assertThat(
                        ((GeneralException) exception).getCode()).isEqualTo(expectedCode));
    }
}
