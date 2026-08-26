package com.example.jejugilmoa.domain.imageupload.service;

import com.example.jejugilmoa.global.external.s3.AwsProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.net.URL;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class S3PresignedImageUrlResolverTest {

    @Mock S3Presigner s3Presigner;
    @Mock PresignedGetObjectRequest presignedRequest;

    @Test
    void resolvesObjectKeyToPresignedGetUrlWithoutObjectLookup() throws Exception {
        AwsProperties properties = new AwsProperties("ap-northeast-2",
                new AwsProperties.S3("record-bucket", 900, 10_485_760));
        given(s3Presigner.presignGetObject(org.mockito.ArgumentMatchers.any(GetObjectPresignRequest.class)))
                .willReturn(presignedRequest);
        given(presignedRequest.url()).willReturn(new URL("https://signed.example/record.jpg"));
        var resolver = new S3PresignedImageUrlResolver(s3Presigner, properties);

        String result = resolver.resolve("records/42/photo.jpg");

        assertThat(result).isEqualTo("https://signed.example/record.jpg");
        ArgumentCaptor<GetObjectPresignRequest> captor = ArgumentCaptor.forClass(GetObjectPresignRequest.class);
        verify(s3Presigner).presignGetObject(captor.capture());
        assertThat(captor.getValue().signatureDuration().toSeconds()).isEqualTo(900);
        assertThat(captor.getValue().getObjectRequest().bucket()).isEqualTo("record-bucket");
        assertThat(captor.getValue().getObjectRequest().key()).isEqualTo("records/42/photo.jpg");
    }
}
