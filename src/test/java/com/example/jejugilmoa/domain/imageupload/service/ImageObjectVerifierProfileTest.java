package com.example.jejugilmoa.domain.imageupload.service;

import com.example.jejugilmoa.global.external.s3.AwsProperties;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import software.amazon.awssdk.services.s3.S3Client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class ImageObjectVerifierProfileTest {

    @ParameterizedTest
    @ValueSource(strings = {"dev", "prod"})
    void nonLocalProfile_selectsOnlyS3Verifier(String profile) {
        try (var context = verifierContext(profile)) {
            assertThat(context.getBeansOfType(ImageObjectVerifier.class))
                    .hasSize(1)
                    .containsValue(context.getBean(S3ImageObjectVerifier.class));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"local"})
    void localProfile_selectsOnlyLocalVerifier(String profile) {
        try (var context = verifierContext(profile)) {
            assertThat(context.getBeansOfType(ImageObjectVerifier.class))
                    .hasSize(1)
                    .containsValue(context.getBean(LocalImageObjectVerifier.class));
        }
    }

    private AnnotationConfigApplicationContext verifierContext(String profile) {
        var context = new AnnotationConfigApplicationContext();
        context.getEnvironment().setActiveProfiles(profile);
        context.registerBean(S3Client.class, () -> mock(S3Client.class));
        context.registerBean(AwsProperties.class, () -> new AwsProperties(
                "ap-northeast-2", new AwsProperties.S3("test-bucket", 900, 10_485_760)));
        context.register(ImageObjectPolicy.class, S3ImageObjectVerifier.class, LocalImageObjectVerifier.class);
        context.refresh();
        return context;
    }
}
