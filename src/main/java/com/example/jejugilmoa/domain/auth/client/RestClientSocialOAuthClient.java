package com.example.jejugilmoa.domain.auth.client;

import com.example.jejugilmoa.domain.auth.config.OAuthProperties;
import com.example.jejugilmoa.domain.auth.dto.GoogleUserInfoResponse;
import com.example.jejugilmoa.domain.auth.dto.KakaoUserInfoResponse;
import com.example.jejugilmoa.domain.auth.dto.NaverUserInfoResponse;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginRequest;
import com.example.jejugilmoa.domain.auth.dto.OAuthProfileResponse;
import com.example.jejugilmoa.domain.auth.dto.OAuthTokenResponse;
import com.example.jejugilmoa.domain.auth.dto.OAuthUserInfo;
import com.example.jejugilmoa.domain.auth.enums.SocialProvider;
import com.example.jejugilmoa.domain.auth.exception.AuthErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestClientSocialOAuthClient implements SocialOAuthClient {

    private static final int MAX_NICKNAME_LENGTH = 50;
    private static final int MAX_PROFILE_IMAGE_URL_LENGTH = 500;

    private final RestClient.Builder restClientBuilder;
    private final OAuthProperties oauthProperties;

    // OAuth 로그인 요청을 처리하고 최종 사용자 정보를 조회한다.
    @Override
    public OAuthUserInfo fetchUserInfo(SocialProvider provider, OAuthLoginRequest request) {
        OAuthProperties.ProviderProperties properties = resolveProperties(provider, request);
        OAuthTokenResponse tokenResponse = requestAccessToken(provider, properties, request);
        if (tokenResponse == null || !StringUtils.hasText(tokenResponse.accessToken())) {
            throw new GeneralException(AuthErrorCode.INVALID_AUTHORIZATION_CODE);
        }

        OAuthProfileResponse profileResponse = requestProfile(provider, properties, tokenResponse.accessToken());
        return toUserInfo(provider, profileResponse);
    }

    // OAuth 제공자별 설정값을 조회하고 필수 설정이 모두 존재하는지 검증한다.
    private OAuthProperties.ProviderProperties resolveProperties(
        SocialProvider provider,
        OAuthLoginRequest request
    ) {
        OAuthProperties.ProviderProperties properties = oauthProperties.get(provider);

        validateProperties(provider, properties, request);

        return properties;
    }

    // OAuth 제공자 설정의 필수 값이 모두 존재하는지 검증한다.
    private void validateProperties(
        SocialProvider provider,
        OAuthProperties.ProviderProperties properties,
        OAuthLoginRequest request
    ) {
        if (properties == null) {
            throw new GeneralException(AuthErrorCode.MISSING_OAUTH_CONFIGURATION);
        }

        if (!StringUtils.hasText(properties.clientId())) {
            throw new GeneralException(AuthErrorCode.MISSING_OAUTH_CLIENT_ID);
        }

        if (provider.isClientSecretRequired()
            && !StringUtils.hasText(properties.clientSecret())) {
            throw new GeneralException(AuthErrorCode.MISSING_OAUTH_CLIENT_SECRET);
        }

        if (!StringUtils.hasText(resolveRedirectUri(properties, request))) {
            throw new GeneralException(AuthErrorCode.MISSING_OAUTH_REDIRECT_URI);
        }

        if (!StringUtils.hasText(properties.tokenUri())) {
            throw new GeneralException(AuthErrorCode.MISSING_OAUTH_TOKEN_URI);
        }

        if (!StringUtils.hasText(properties.userInfoUri())) {
            throw new GeneralException(AuthErrorCode.MISSING_OAUTH_USER_INFO_URI);
        }
    }

    // 인가 코드를 이용해 OAuth 제공자로부터 Access Token을 발급받는다.
    private OAuthTokenResponse requestAccessToken(
        SocialProvider provider,
        OAuthProperties.ProviderProperties properties,
        OAuthLoginRequest request
    ) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("client_id", properties.clientId());
        form.add("code", request.authorizationCode());
        form.add("redirect_uri", resolveRedirectUri(properties, request));

        if (StringUtils.hasText(properties.clientSecret())) {
            form.add("client_secret", properties.clientSecret());
        }
        if (provider == SocialProvider.NAVER && StringUtils.hasText(request.state())) {
            form.add("state", request.state());
        }

        try {
            return restClientBuilder.build()
                .post()
                .uri(properties.tokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .body(form)
                .retrieve()
                .body(OAuthTokenResponse.class);
        } catch (RestClientResponseException ex) {
            log.warn("OAuth token request failed. provider={}, status={}",
                provider.getKey(), ex.getStatusCode());
            if (ex.getStatusCode().is4xxClientError()) {
                throw new GeneralException(AuthErrorCode.INVALID_AUTHORIZATION_CODE);
            }
            throw new GeneralException(AuthErrorCode.OAUTH_PROVIDER_ERROR);
        } catch (RestClientException ex) {
            log.warn("OAuth token request failed. provider={}", provider.getKey(), ex);
            throw new GeneralException(AuthErrorCode.OAUTH_PROVIDER_ERROR);
        }
    }

    // Access Token을 이용해 OAuth 제공자의 사용자 프로필 정보를 조회한다.
    private OAuthProfileResponse requestProfile(
        SocialProvider provider,
        OAuthProperties.ProviderProperties properties,
        String accessToken
    ) {
        try {
            RestClient restClient = restClientBuilder.build();
            return switch (provider) {
                case KAKAO -> {
                    KakaoUserInfoResponse response = restClient.get()
                        .uri(properties.userInfoUri())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .body(KakaoUserInfoResponse.class);
                    yield response == null ? null : response.toProfileResponse();
                }
                case NAVER -> {
                    NaverUserInfoResponse response = restClient.get()
                        .uri(properties.userInfoUri())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .body(NaverUserInfoResponse.class);
                    yield response == null ? null : response.toProfileResponse();
                }
                case GOOGLE -> {
                    GoogleUserInfoResponse response = restClient.get()
                        .uri(properties.userInfoUri())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .retrieve()
                        .body(GoogleUserInfoResponse.class);
                    yield response == null ? null : response.toProfileResponse();
                }
            };
        } catch (RestClientException ex) {
            log.warn("OAuth profile request failed. provider={}", provider.getKey(), ex);
            throw new GeneralException(AuthErrorCode.OAUTH_PROVIDER_ERROR);
        }
    }

    // 제공자별 프로필 정보를 서비스에서 사용하는 OAuthUserInfo로 변환한다.
    private OAuthUserInfo toUserInfo(SocialProvider provider, OAuthProfileResponse response) {
        if (response == null || !StringUtils.hasText(response.externalId())) {
            throw new GeneralException(AuthErrorCode.INVALID_OAUTH_PROFILE);
        }

        return new OAuthUserInfo(
            provider,
            response.externalId(),
            normalizeNickname(response.nickname(), provider),
            trimToMax(response.profileImageUrl(), MAX_PROFILE_IMAGE_URL_LENGTH)
        );
    }

    // 요청값이 있으면 해당 Redirect URI를, 없으면 기본 설정값을 사용한다.
    private String resolveRedirectUri(OAuthProperties.ProviderProperties properties, OAuthLoginRequest request) {
        if (StringUtils.hasText(request.redirectUri())) {
            return request.redirectUri();
        }
        return properties.redirectUri();
    }

    // 닉네임이 없으면 기본값을 생성하고 길이를 제한한다.
    private String normalizeNickname(String nickname, SocialProvider provider) {
        if (!StringUtils.hasText(nickname)) {
            return provider.getDisplayName() + " 사용자";
        }
        return trimToMax(nickname.trim(), MAX_NICKNAME_LENGTH);
    }

    // 문자열의 앞뒤 공백을 제거하고 최대 길이를 초과하면 잘라낸다.
    private String trimToMax(String value, int maxLength) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.length() <= maxLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxLength);
    }
}
