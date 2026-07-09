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
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.enums.Role;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
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
    private final JwtProvider jwtProvider;
    private final RefreshTokenRepository refreshTokenRepository;

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
            .map(user -> AuthConverter.toResponse(user, false))
            .orElseGet(() -> createUser(userInfo));
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

    // 리프레시 토큰 회전: 제시된 토큰을 즉시 폐기하고 새 access/refresh 쌍을 발급한다.
    // 이미 폐기된(revoked) 토큰이 재사용되면 탈취로 간주해 해당 유저의 모든 토큰을 무효화한다.
    // GeneralException은 롤백 대상에서 제외한다 — REFRESH_TOKEN_REUSED로 던지기 직전에 실행한
    // revokeAllByUserId(방어적 전체 무효화)가 예외 때문에 같이 롤백되지 않고 실제로 커밋되어야 한다.
    @Transactional(noRollbackFor = GeneralException.class)
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
    public void logout(String refreshTokenValue) {
        if (refreshTokenValue == null || refreshTokenValue.isBlank()) {
            return;
        }
        try {
            Claims claims = jwtProvider.parseRefreshClaims(refreshTokenValue);
            refreshTokenRepository.findByTokenId(claims.getId()).ifPresent(RefreshToken::revoke);
        } catch (GeneralException ignored) {
            // 이미 만료되었거나 위조된 토큰은 어차피 재사용할 수 없으므로 무시하고 로그아웃 처리한다.
        }
    }
}
