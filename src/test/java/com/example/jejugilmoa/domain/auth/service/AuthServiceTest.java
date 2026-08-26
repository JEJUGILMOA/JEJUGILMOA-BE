package com.example.jejugilmoa.domain.auth.service;

import com.example.jejugilmoa.domain.auth.client.SocialOAuthClient;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginRequest;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginResponse;
import com.example.jejugilmoa.domain.auth.dto.OAuthUserInfo;
import com.example.jejugilmoa.domain.auth.entity.RefreshToken;
import com.example.jejugilmoa.domain.auth.enums.SocialProvider;
import com.example.jejugilmoa.domain.auth.exception.AuthErrorCode;
import com.example.jejugilmoa.domain.auth.jwt.JwtProvider;
import com.example.jejugilmoa.domain.auth.jwt.TokenPair;
import com.example.jejugilmoa.domain.auth.repository.RefreshTokenRepository;
import com.example.jejugilmoa.domain.notification.entity.DeviceToken;
import com.example.jejugilmoa.domain.notification.repository.DeviceTokenRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.enums.Role;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SocialOAuthClient socialOAuthClient;

    @Mock
    private UserRepository userRepository;

    @Mock
    private JwtProvider jwtProvider;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private DeviceTokenRepository deviceTokenRepository;

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
                "https://example.com/profile.png",
                "jeju@example.com"
        );
        User savedUser = User.builder()
                .id(1L)
                .externalProvider("kakao")
                .externalId("12345")
                .nickname("제주러")
                .profileImageUrl("https://example.com/profile.png")
                .email("jeju@example.com")
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
                null,
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
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("신규 유저 생성 시 소셜 프로필의 닉네임/이미지/이메일을 함께 저장한다")
    void loginPersistsSocialProfileFieldsForNewUser() {
        OAuthLoginRequest request = new OAuthLoginRequest("auth-code", "http://localhost/callback", null);
        OAuthUserInfo userInfo = new OAuthUserInfo(
                SocialProvider.NAVER,
                "naver-id",
                "네이버러",
                "https://example.com/naver.png",
                "naver@example.com"
        );

        given(socialOAuthClient.fetchUserInfo(SocialProvider.NAVER, request)).willReturn(userInfo);
        given(userRepository.findByExternalProviderAndExternalIdAndDeletedAtIsNull("naver", "naver-id"))
                .willReturn(Optional.empty());
        given(userRepository.save(any(User.class))).willAnswer(invocation -> invocation.getArgument(0));

        authService.login("naver", request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getNickname()).isEqualTo("네이버러");
        assertThat(saved.getProfileImageUrl()).isEqualTo("https://example.com/naver.png");
        assertThat(saved.getEmail()).isEqualTo("naver@example.com");
    }

    @Test
    @DisplayName("기존 유저의 이메일이 비어 있으면 소셜 프로필의 이메일로 채워 저장한다")
    void loginBackfillsEmailForExistingUserWithoutEmail() {
        OAuthLoginRequest request = new OAuthLoginRequest("auth-code", "http://localhost/callback", null);
        OAuthUserInfo userInfo = new OAuthUserInfo(
                SocialProvider.KAKAO,
                "12345",
                "제주러",
                null,
                "late-consent@example.com"
        );
        User existingUser = User.builder()
                .id(3L)
                .externalProvider("kakao")
                .externalId("12345")
                .nickname("기존유저")
                .build();

        given(socialOAuthClient.fetchUserInfo(SocialProvider.KAKAO, request)).willReturn(userInfo);
        given(userRepository.findByExternalProviderAndExternalIdAndDeletedAtIsNull("kakao", "12345"))
                .willReturn(Optional.of(existingUser));

        authService.login("kakao", request);

        assertThat(existingUser.getEmail()).isEqualTo("late-consent@example.com");
        verify(userRepository).save(existingUser);
    }

    @Test
    @DisplayName("기존 유저에 이미 이메일이 있으면 소셜 프로필의 이메일로 덮어쓰지 않는다")
    void loginKeepsExistingEmail() {
        OAuthLoginRequest request = new OAuthLoginRequest("auth-code", "http://localhost/callback", null);
        OAuthUserInfo userInfo = new OAuthUserInfo(
                SocialProvider.KAKAO,
                "12345",
                "제주러",
                null,
                "new@example.com"
        );
        User existingUser = User.builder()
                .id(3L)
                .externalProvider("kakao")
                .externalId("12345")
                .nickname("기존유저")
                .email("old@example.com")
                .build();

        given(socialOAuthClient.fetchUserInfo(SocialProvider.KAKAO, request)).willReturn(userInfo);
        given(userRepository.findByExternalProviderAndExternalIdAndDeletedAtIsNull("kakao", "12345"))
                .willReturn(Optional.of(existingUser));

        authService.login("kakao", request);

        assertThat(existingUser.getEmail()).isEqualTo("old@example.com");
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("토큰을 발급하면 액세스/리프레시 토큰을 생성하고 리프레시 토큰을 저장한다")
    void issueTokensGeneratesAndPersistsRefreshToken() {
        given(jwtProvider.generateAccessToken(eq(1L), eq(Role.USER))).willReturn("access-token");
        given(jwtProvider.generateRefreshToken(eq(1L), any())).willReturn("refresh-token");
        given(jwtProvider.getRefreshTokenExpiry()).willReturn(Instant.now().plusSeconds(60));

        TokenPair tokens = authService.issueTokens(1L, Role.USER);

        assertThat(tokens.accessToken()).isEqualTo("access-token");
        assertThat(tokens.refreshToken()).isEqualTo("refresh-token");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("유효한 리프레시 토큰으로 재발급하면 기존 토큰을 폐기하고 새 토큰 쌍을 발급한다")
    void reissueRotatesRefreshToken() {
        Claims claims = mock(Claims.class);
        given(jwtProvider.parseRefreshClaims("old-refresh")).willReturn(claims);
        given(jwtProvider.getUserId(claims)).willReturn(1L);
        given(claims.getId()).willReturn("jti-1");

        RefreshToken savedToken = RefreshToken.builder()
                .userId(1L)
                .tokenId("jti-1")
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        given(refreshTokenRepository.findByTokenId("jti-1")).willReturn(Optional.of(savedToken));

        User user = User.builder().id(1L).nickname("제주러").role(Role.USER).build();
        given(userRepository.findByIdAndDeletedAtIsNull(1L)).willReturn(Optional.of(user));

        given(jwtProvider.generateAccessToken(eq(1L), eq(Role.USER))).willReturn("new-access");
        given(jwtProvider.generateRefreshToken(eq(1L), any())).willReturn("new-refresh");
        given(jwtProvider.getRefreshTokenExpiry()).willReturn(Instant.now().plusSeconds(60));

        TokenPair tokens = authService.reissue("old-refresh");

        assertThat(tokens.accessToken()).isEqualTo("new-access");
        assertThat(tokens.refreshToken()).isEqualTo("new-refresh");
        assertThat(savedToken.isRevoked()).isTrue();
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    @DisplayName("이미 폐기된 리프레시 토큰이 재사용되면 해당 유저의 모든 토큰을 무효화하고 예외를 던진다")
    void reissueDetectsReuseAndRevokesAllUserTokens() {
        Claims claims = mock(Claims.class);
        given(jwtProvider.parseRefreshClaims("stolen-token")).willReturn(claims);
        given(jwtProvider.getUserId(claims)).willReturn(1L);
        given(claims.getId()).willReturn("jti-1");

        RefreshToken revokedToken = RefreshToken.builder()
                .userId(1L)
                .tokenId("jti-1")
                .expiresAt(Instant.now().plusSeconds(60))
                .revoked(true)
                .build();
        given(refreshTokenRepository.findByTokenId("jti-1")).willReturn(Optional.of(revokedToken));

        assertThatThrownBy(() -> authService.reissue("stolen-token"))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.REFRESH_TOKEN_REUSED);

        verify(refreshTokenRepository).revokeAllByUserId(1L);
    }

    @Test
    @DisplayName("동시 재발급 요청으로 낙관적 락이 충돌하면 재사용으로 간주해 전체 토큰을 무효화한다")
    void reissueDetectsConcurrentRaceViaOptimisticLock() {
        Claims claims = mock(Claims.class);
        given(jwtProvider.parseRefreshClaims("racy-token")).willReturn(claims);
        given(jwtProvider.getUserId(claims)).willReturn(1L);
        given(claims.getId()).willReturn("jti-1");

        RefreshToken savedToken = RefreshToken.builder()
                .userId(1L)
                .tokenId("jti-1")
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        given(refreshTokenRepository.findByTokenId("jti-1")).willReturn(Optional.of(savedToken));
        given(refreshTokenRepository.saveAndFlush(savedToken))
                .willThrow(new ObjectOptimisticLockingFailureException(RefreshToken.class, "jti-1"));

        assertThatThrownBy(() -> authService.reissue("racy-token"))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.REFRESH_TOKEN_REUSED);

        verify(refreshTokenRepository).revokeAllByUserId(1L);
    }

    @Test
    @DisplayName("리프레시 토큰 쿠키가 없으면 재발급은 예외를 던진다")
    void reissueFailsWhenRefreshTokenMissing() {
        assertThatThrownBy(() -> authService.reissue(null))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode())
                .isEqualTo(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);

        verifyNoInteractions(jwtProvider, refreshTokenRepository);
    }

    @Test
    @DisplayName("로그아웃하면 저장된 리프레시 토큰을 폐기한다")
    void logoutRevokesStoredRefreshToken() {
        Claims claims = mock(Claims.class);
        given(jwtProvider.parseRefreshClaims("refresh-token")).willReturn(claims);
        given(claims.getId()).willReturn("jti-1");

        RefreshToken savedToken = RefreshToken.builder()
                .userId(1L)
                .tokenId("jti-1")
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        given(refreshTokenRepository.findByTokenId("jti-1")).willReturn(Optional.of(savedToken));

        authService.logout("refresh-token", null);

        assertThat(savedToken.isRevoked()).isTrue();
    }

    @Test
    @DisplayName("리프레시 토큰이 없으면 로그아웃은 아무 것도 하지 않는다")
    void logoutNoOpWhenRefreshTokenMissing() {
        authService.logout(null, null);

        verifyNoInteractions(jwtProvider, refreshTokenRepository);
    }

    @Test
    @DisplayName("유효한 리프레시 토큰과 deviceId를 전달하면 일치하는 디바이스 토큰을 조회하고 삭제한다")
    void logoutWithDeviceIdDeletesMatchingToken() {
        Claims claims = mock(Claims.class);
        given(jwtProvider.parseRefreshClaims("refresh-token")).willReturn(claims);
        given(jwtProvider.getUserId(claims)).willReturn(1L);
        given(claims.getId()).willReturn("jti-1");

        RefreshToken savedToken = RefreshToken.builder()
                .userId(1L)
                .tokenId("jti-1")
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        given(refreshTokenRepository.findByTokenId("jti-1")).willReturn(Optional.of(savedToken));

        DeviceToken deviceToken = mock(DeviceToken.class);
        given(deviceTokenRepository.findByUserIdAndDeviceId(1L, "device-abc"))
                .willReturn(Optional.of(deviceToken));

        authService.logout("refresh-token", "device-abc");

        assertThat(savedToken.isRevoked()).isTrue();
        verify(deviceTokenRepository).findByUserIdAndDeviceId(1L, "device-abc");
        verify(deviceTokenRepository).delete(deviceToken);
    }

    @Test
    @DisplayName("deviceId에 일치하는 디바이스 토큰이 없으면 삭제를 호출하지 않는다")
    void logoutWithDeviceIdNoMatchSkipsDelete() {
        Claims claims = mock(Claims.class);
        given(jwtProvider.parseRefreshClaims("refresh-token")).willReturn(claims);
        given(jwtProvider.getUserId(claims)).willReturn(1L);
        given(claims.getId()).willReturn("jti-1");

        RefreshToken savedToken = RefreshToken.builder()
                .userId(1L)
                .tokenId("jti-1")
                .expiresAt(Instant.now().plusSeconds(60))
                .build();
        given(refreshTokenRepository.findByTokenId("jti-1")).willReturn(Optional.of(savedToken));

        given(deviceTokenRepository.findByUserIdAndDeviceId(1L, "device-xyz"))
                .willReturn(Optional.empty());

        authService.logout("refresh-token", "device-xyz");

        verify(deviceTokenRepository).findByUserIdAndDeviceId(1L, "device-xyz");
        verify(deviceTokenRepository, never()).delete(any(DeviceToken.class));
    }
}
