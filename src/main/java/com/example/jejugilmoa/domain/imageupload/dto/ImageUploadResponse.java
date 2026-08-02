package com.example.jejugilmoa.domain.imageupload.dto;

import java.time.Instant;
import java.util.Map;

public record ImageUploadResponse(
        String objectKey,
        String uploadUrl,
        String httpMethod,
        Map<String, String> requiredHeaders,
        Instant expiresAt
) {
}
