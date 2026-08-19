package com.example.jejugilmoa.domain.imageupload.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

import com.example.jejugilmoa.domain.imageupload.dto.ImageUploadRequest;
import com.example.jejugilmoa.domain.imageupload.exception.ImageUploadErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.example.jejugilmoa.global.external.s3.AwsProperties;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

@ExtendWith(MockitoExtension.class)
class ImageUploadServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-02T00:00:00Z");

    @Mock S3Presigner s3Presigner;
    @Mock PresignedPutObjectRequest presignedPutObjectRequest;
    private ImageUploadService imageUploadService;

    @BeforeEach
    void setUp() throws Exception {
        var properties = new AwsProperties("ap-northeast-2", new AwsProperties.S3("test-bucket", 900, 10_485_760));
        imageUploadService = new ImageUploadService(s3Presigner, properties, Clock.fixed(NOW, ZoneOffset.UTC),
                new ImageObjectPolicy(properties));
    }

    @ParameterizedTest
    @CsvSource({"image/jpeg,jpg", "image/png,png", "image/webp,webp"})
    void createUploadUrl_supportsImageTypes(String contentType, String extension) {
        stubPresignedUrl();
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .willReturn(presignedPutObjectRequest);

        var response = imageUploadService.createUploadUrl(new ImageUploadRequest(contentType, 3_145_728L), 42L);

        assertThat(response.objectKey())
                .matches("records/42/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\." + extension);
        assertThat(response.uploadUrl()).isEqualTo("https://test-bucket.s3.amazonaws.com/upload");
        assertThat(response.httpMethod()).isEqualTo("PUT");
        assertThat(response.requiredHeaders()).containsEntry("Content-Type", contentType);
        assertThat(response.expiresAt()).isEqualTo(NOW.plusSeconds(900));

        var captor = ArgumentCaptor.forClass(PutObjectPresignRequest.class);
        org.mockito.Mockito.verify(s3Presigner).presignPutObject(captor.capture());
        assertThat(captor.getValue().signatureDuration()).isEqualTo(Duration.ofSeconds(900));
        assertThat(captor.getValue().putObjectRequest().bucket()).isEqualTo("test-bucket");
        assertThat(captor.getValue().putObjectRequest().contentType()).isEqualTo(contentType);
        assertThat(captor.getValue().putObjectRequest().acl()).isNull();
    }

    @Test
    void createUploadUrl_doesNotUseOriginalNameOrClientKey() {
        stubPresignedUrl();
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .willReturn(presignedPutObjectRequest);

        var request = new ImageUploadRequest("image/jpeg", 1L);
        var response = imageUploadService.createUploadUrl(request, 7L);

        assertThat(response.objectKey()).startsWith("records/7/").doesNotContain("original", "filename");
        assertThat(ImageUploadRequest.class.getRecordComponents())
                .extracting(component -> component.getName())
                .containsExactly("contentType", "fileSize");
    }

    @Test
    void createUploadUrl_rejectsUnsupportedType() {
        assertError(new ImageUploadRequest("image/gif", 1L), ImageUploadErrorCode.UNSUPPORTED_CONTENT_TYPE);
    }

    @Test
    void createUploadUrl_rejectsOversizedFile() {
        assertError(new ImageUploadRequest("image/jpeg", 10_485_761L), ImageUploadErrorCode.INVALID_FILE_SIZE);
    }

    @Test
    void createUploadUrl_rejectsNonPositiveFile() {
        assertError(new ImageUploadRequest("image/jpeg", 0L), ImageUploadErrorCode.INVALID_FILE_SIZE);
    }

    @Test
    void createUploadUrl_rejectsMissingAuthenticatedUser() {
        assertThatThrownBy(() -> imageUploadService.createUploadUrl(new ImageUploadRequest("image/jpeg", 1L), null))
                .isInstanceOf(GeneralException.class)
                .extracting(exception -> ((GeneralException) exception).getCode())
                .isEqualTo(ImageUploadErrorCode.AUTHENTICATED_USER_NOT_FOUND);
    }

    @Test
    void createUploadUrl_wrapsPresignFailure() {
        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class)))
                .willThrow(new IllegalStateException("AWS failure"));

        assertError(new ImageUploadRequest("image/jpeg", 1L), ImageUploadErrorCode.PRESIGN_FAILED);
    }

    private void assertError(ImageUploadRequest request, ImageUploadErrorCode expectedCode) {
        assertThatThrownBy(() -> imageUploadService.createUploadUrl(request, 42L))
                .isInstanceOf(GeneralException.class)
                .extracting(exception -> ((GeneralException) exception).getCode())
                .isEqualTo(expectedCode);
    }

    private void stubPresignedUrl() {
        try {
            given(presignedPutObjectRequest.url()).willReturn(
                    java.net.URI.create("https://test-bucket.s3.amazonaws.com/upload").toURL());
        } catch (java.net.MalformedURLException exception) {
            throw new IllegalStateException(exception);
        }
    }

}
