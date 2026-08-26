package com.example.jejugilmoa.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GoogleUserInfoResponse(
    String sub, // 사용자의 고유 ID
    String name,
    String picture,
    String email
) {

    public OAuthProfileResponse toProfileResponse() {
        return new OAuthProfileResponse(sub, name, picture, email);
    }
}
