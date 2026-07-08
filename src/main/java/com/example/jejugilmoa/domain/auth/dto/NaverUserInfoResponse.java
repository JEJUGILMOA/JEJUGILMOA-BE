package com.example.jejugilmoa.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record NaverUserInfoResponse(
    NaverProfile response
) {

    public OAuthProfileResponse toProfileResponse() {
        return new OAuthProfileResponse(
            response == null ? null : response.id(),
            response == null ? null : response.nickname(),
            response == null ? null : response.profileImageUrl()
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record NaverProfile(
        String id,
        String nickname,

        @JsonProperty("profile_image")
        String profileImageUrl
    ) {
    }
}
