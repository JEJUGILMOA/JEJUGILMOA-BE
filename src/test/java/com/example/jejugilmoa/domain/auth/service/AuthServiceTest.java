package com.example.jejugilmoa.domain.auth.service;

import com.example.jejugilmoa.domain.auth.client.SocialOAuthClient;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginRequest;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginResponse;
import com.example.jejugilmoa.domain.auth.dto.OAuthUserInfo;
import com.example.jejugilmoa.domain.auth.enums.SocialProvider;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SocialOAuthClient socialOAuthClient;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("소셜 계정이 없으면 새 사용자를 생성하고 로그인 응답을 반환한다")
    void loginCreatesUserWhenSocialAccountNotExists() {
        OAuthLoginRequest request = new OAuthLoginRequest("auth-code", "http://localhost/callback", null);
        OAuthUserInfo userInfo = new OAuthUserInfo(
                SocialProvider.KAKAO,
                "12345",
                "제주러",
                "https://example.com/profile.png"
        );
        User savedUser = User.builder()
                .id(1L)
                .externalProvider("kakao")
                .externalId("12345")
                .nickname("제주러")
                .profileImageUrl("https://example.com/profile.png")
                .build();

        given(socialOAuthClient.fetchUserInfo(SocialProvider.KAKAO, request)).willReturn(userInfo);
        given(userRepository.findByExternalProviderAndExternalIdAndDeletedAtIsNull("kakao", "12345"))
                .willReturn(Optional.empty());
        given(userRepository.save(org.mockito.ArgumentMatchers.any(User.class))).willReturn(savedUser);

        OAuthLoginResponse response = authService.login("kakao", request);

        assertThat(response.userId()).isEqualTo(1L);
        assertThat(response.nickname()).isEqualTo("제주러");
        assertThat(response.newUser()).isTrue();
    }

    @Test
    @DisplayName("소셜 계정이 있으면 기존 사용자를 반환한다")
    void loginReturnsExistingUserWhenSocialAccountExists() {
        OAuthLoginRequest request = new OAuthLoginRequest("auth-code", "http://localhost/callback", null);
        OAuthUserInfo userInfo = new OAuthUserInfo(
                SocialProvider.GOOGLE,
                "google-sub",
                "구글러",
                null
        );
        User existingUser = User.builder()
                .id(2L)
                .externalProvider("google")
                .externalId("google-sub")
                .nickname("기존유저")
                .build();

        given(socialOAuthClient.fetchUserInfo(SocialProvider.GOOGLE, request)).willReturn(userInfo);
        given(userRepository.findByExternalProviderAndExternalIdAndDeletedAtIsNull("google", "google-sub"))
                .willReturn(Optional.of(existingUser));

        OAuthLoginResponse response = authService.login("google", request);

        assertThat(response.userId()).isEqualTo(2L);
        assertThat(response.nickname()).isEqualTo("기존유저");
        assertThat(response.newUser()).isFalse();
        verify(userRepository).findByExternalProviderAndExternalIdAndDeletedAtIsNull("google", "google-sub");
    }
}
