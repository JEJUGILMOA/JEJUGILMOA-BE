package com.example.jejugilmoa.global.external.s3;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "aws")
public record AwsProperties(String region, S3 s3) {

    public record S3(String bucket, long presignExpirationSeconds, long maxImageSizeBytes) {
    }
}
