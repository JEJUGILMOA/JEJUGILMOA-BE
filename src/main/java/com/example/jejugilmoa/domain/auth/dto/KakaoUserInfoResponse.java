package com.example.jejugilmoa.domain.auth.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record KakaoUserInfoResponse(

    Long id,

    @JsonProperty("kakao_account")
    KakaoAccount kakaoAccount
) {

    public OAuthProfileResponse toProfileResponse() {
        KakaoProfile profile = kakaoAccount == null ? null : kakaoAccount.profile();
        return new OAuthProfileResponse(
            id == null ? null : String.valueOf(id),
            profile == null ? null : profile.nickname(),
            profile == null ? null : profile.profileImageUrl()
        );
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoAccount(
        KakaoProfile profile
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record KakaoProfile(
        String nickname,

        @JsonProperty("profile_image_url")
        String profileImageUrl
    ) {
    }
}
