package com.example.jejugilmoa.domain.auth.service;

import com.example.jejugilmoa.domain.auth.client.SocialOAuthClient;
import com.example.jejugilmoa.domain.auth.converter.AuthConverter;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginRequest;
import com.example.jejugilmoa.domain.auth.dto.OAuthLoginResponse;
import com.example.jejugilmoa.domain.auth.dto.OAuthUserInfo;
import com.example.jejugilmoa.domain.auth.entity.RefreshToken;
import com.example.jejugilmoa.domain.auth.enums.SocialProvider;
import com.example.jejugilmoa.domain.auth.exception.AuthErrorCode;
import com.example.jejugilmoa.domain.auth.jwt.JwtProvider;
import com.example.jejugilmoa.domain.auth.jwt.TokenPair;
import com.example.jejugilmoa.domain.auth.repository.RefreshTokenRepository;
import com.example.jejugilmoa.domain.notification.repository.DeviceTokenRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.enums.Role;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.domain.user.service.UserService;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final SocialOAuthClient socialOAuthClient;
    private final UserRepository userRepository;
    private final UserService userService;
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final DeviceTokenRepository deviceTokenRepository;

    // 전체를 하나의 트랜잭션으로 묶지 않는다. 동시 로그인 경합으로 createUser()가
    // 유니크 제약을 위반하면 재조회로 복구하는데, 같은 트랜잭션(커넥션) 안에서는
    // 위반 시점에 이미 커넥션이 abort 상태라 재조회 자체가 실패한다.
    // find/save 각각은 스프링 데이터 JPA가 별도 트랜잭션으로 처리해준다.
    public OAuthLoginResponse login(String providerValue, OAuthLoginRequest request) {
        SocialProvider provider = SocialProvider.from(providerValue);
        OAuthUserInfo userInfo = socialOAuthClient.fetchUserInfo(provider, request);

        try {
            return findOrCreateUser(userInfo);
        } catch (DataIntegrityViolationException ex) {
            // 두 요청이 동시에 신규 유저로 판단해 저장을 시도하면 uk_user_provider_external_id
            // 제약을 나중에 커밋한 쪽이 위반한다. 새 트랜잭션으로 재조회하면 먼저 커밋된
            // 유저를 찾을 수 있으므로 한 번만 재시도한다.
            return findOrCreateUser(userInfo);
        }
    }

    private OAuthLoginResponse findOrCreateUser(OAuthUserInfo userInfo) {
        return userRepository.findByExternalProviderAndExternalIdAndDeletedAtIsNull(
                userInfo.provider().getKey(),
                userInfo.externalId()
            )
            .map(user -> {
                backfillEmailIfAbsent(user, userInfo);
                return AuthConverter.toResponse(user, false);
            })
            .orElseGet(() -> restoreOrCreateUser(userInfo));
    }

    // 같은 provider/externalId로 탈퇴한(그러나 아직 익명화되지 않은, 즉 탈퇴 30일 이내) 계정이
    // 남아있으면 새 계정을 만드는 대신 복구한다. 30일이 지나 익명화된 계정은 provider/externalId가
    // 비워지므로 이 조회에 걸리지 않고 자연스럽게 새 계정 생성으로 이어진다.
    // 복구는 UserService의 트랜잭션 안에서 행 잠금과 함께 수행되어 익명화 스케줄러와 직렬화된다.
    private OAuthLoginResponse restoreOrCreateUser(OAuthUserInfo userInfo) {
        return userService.restoreByExternalAccount(
                userInfo.provider().getKey(),
                userInfo.externalId()
            )
            .map(user -> {
                backfillEmailIfAbsent(user, userInfo);
                return AuthConverter.toResponse(user, false);
            })
            .orElseGet(() -> createUser(userInfo));
    }

    // 이메일 동의항목이 나중에 추가된 기존 유저는 email이 비어 있으므로 로그인 시 채워준다.
    // 닉네임/프로필 이미지는 유저가 서비스 내에서 수정할 수 있으므로 덮어쓰지 않는다.
    // 이 메서드는 트랜잭션 밖에서 호출되므로 더티 체킹이 동작하지 않는다 — 명시적으로 save한다.
    private void backfillEmailIfAbsent(User user, OAuthUserInfo userInfo) {
        if (user.getEmail() == null && userInfo.email() != null) {
            user.updateEmail(userInfo.email());
            userRepository.save(user);
        }
    }

    private OAuthLoginResponse createUser(OAuthUserInfo userInfo) {
        User user = userRepository.save(AuthConverter.toUser(userInfo));
        return AuthConverter.toResponse(user, true);
    }

    @Transactional
    public TokenPair issueTokens(Long userId, Role role) {
        String tokenId = UUID.randomUUID().toString();
        String accessToken = jwtProvider.generateAccessToken(userId, role);
        String refreshToken = jwtProvider.generateRefreshToken(userId, tokenId);

        refreshTokenRepository.save(RefreshToken.builder()
            .userId(userId)
            .tokenId(tokenId)
            .expiresAt(jwtProvider.getRefreshTokenExpiry())
            .build());

        return new TokenPair(accessToken, refreshToken);
    }

    // Refresh Token 회전(Rotation)
    // - 정상 요청: 기존 Refresh Token을 폐기하고 새 토큰 쌍을 발급한다.
    // - 이미 폐기된 토큰이 다시 들어오면 탈취로 간주하고,
    //   해당 사용자의 모든 Refresh Token을 폐기한 뒤 예외를 발생시킨다.
    //
    // revokeAllByUserId()는 REQUIRES_NEW로 별도 트랜잭션에서 커밋되므로,
    // 이후 현재 트랜잭션이 롤백되어도 토큰 전체 폐기 결과는 유지된다.
    @Transactional
    public TokenPair reissue(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            throw new GeneralException(AuthErrorCode.REFRESH_TOKEN_NOT_FOUND);
        }

        Claims claims = jwtProvider.parseRefreshClaims(refreshTokenValue);
        Long userId = jwtProvider.getUserId(claims);
        String tokenId = claims.getId();

        RefreshToken savedToken = refreshTokenRepository.findByTokenId(tokenId)
            .orElseThrow(() -> new GeneralException(AuthErrorCode.INVALID_TOKEN));

        if (savedToken.isRevoked()) {
            refreshTokenRepository.revokeAllByUserId(userId);
            throw new GeneralException(AuthErrorCode.REFRESH_TOKEN_REUSED);
        }
        savedToken.revoke();

        try {
            // 동일한 토큰으로 동시에 들어온 요청이 있다면 여기서 낙관적 락 충돌로 즉시 드러난다.
            // saveAndFlush로 커밋을 기다리지 않고 이 자리에서 검증한다.
            refreshTokenRepository.saveAndFlush(savedToken);
        } catch (OptimisticLockingFailureException e) {
            refreshTokenRepository.revokeAllByUserId(userId);
            throw new GeneralException(AuthErrorCode.REFRESH_TOKEN_REUSED);
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
            .orElseThrow(() -> new GeneralException(AuthErrorCode.INVALID_TOKEN));

        return issueTokens(user.getId(), user.getRole());
    }

    @Transactional
    public void logout(String refreshTokenValue, String deviceId) {
        Long resolvedUserId = null;
        if (refreshTokenValue != null && !refreshTokenValue.isBlank()) {
            try {
                Claims claims = jwtProvider.parseRefreshClaims(refreshTokenValue);
                resolvedUserId = jwtProvider.getUserId(claims);
                refreshTokenRepository.findByTokenId(claims.getId()).ifPresent(RefreshToken::revoke);
            } catch (GeneralException ignored) {
                // 이미 만료되었거나 위조된 토큰은 어차피 재사용할 수 없으므로 무시하고 로그아웃 처리한다.
            }
        }
        if (resolvedUserId != null && deviceId != null) {
            deviceTokenRepository.findByUserIdAndDeviceId(resolvedUserId, deviceId)
                    .ifPresent(deviceTokenRepository::delete);
        }
    }
}
