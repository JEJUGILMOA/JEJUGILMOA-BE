package com.example.jejugilmoa.domain.auth.verifier;

import com.example.jejugilmoa.domain.auth.client.AppleJwksClient;
import com.example.jejugilmoa.domain.auth.config.AppleAuthProperties;
import com.example.jejugilmoa.domain.auth.dto.AppleIdentityClaims;
import com.example.jejugilmoa.domain.auth.exception.AuthErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

@Component
@RequiredArgsConstructor
public class AppleJwtIdentityTokenVerifier implements AppleIdentityTokenVerifier {
    private final AppleJwksClient jwksClient;
    private final AppleAuthProperties properties;
    private final AppleIdentityTokenReplayStore replayStore;

    @Override
    public AppleIdentityClaims verify(String identityToken, String rawNonce) {
        validateConfiguration();
        if (!StringUtils.hasText(identityToken) || identityToken.length() > 16384
                || !StringUtils.hasText(rawNonce) || rawNonce.length() < 32 || rawNonce.length() > 256) {
            throw new GeneralException(AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
        }
        try {
            Claims claims = Jwts.parser()
                    .keyLocator(header -> {
                        if (!(header instanceof JwsHeader jws) || !"RS256".equals(jws.getAlgorithm())
                                || !StringUtils.hasText(jws.getKeyId()) || jws.getKeyId().length() > 256
                                || header.containsKey("zip")) {
                            throw new GeneralException(AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
                        }
                        // header의 jku/x5u는 사용하지 않고 서버 설정의 JWKS만 조회한다.
                        return jwksClient.getKey(jws.getKeyId());
                    })
                    .requireIssuer(properties.issuer())
                    .build().parseSignedClaims(identityToken).getPayload();

            if (claims.getExpiration() == null || claims.getSubject() == null
                    || claims.getSubject().isBlank() || claims.getSubject().length() > 255
                    || claims.getAudience() == null
                    || claims.getAudience().stream().noneMatch(properties.allowedAudiences()::contains)) {
                throw new GeneralException(AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
            }
            // 만료 시각과 정확히 같은 순간도 거부한다. 서버는 UTC/NTP 동기화를 전제로 한다.
            if (!claims.getExpiration().toInstant().isAfter(Instant.now())) {
                throw new GeneralException(AuthErrorCode.EXPIRED_APPLE_IDENTITY_TOKEN);
            }
            String nonce = claims.get("nonce", String.class);
            String expectedNonce = sha256(rawNonce);
            if (nonce == null || !MessageDigest.isEqual(expectedNonce.getBytes(StandardCharsets.UTF_8),
                    nonce.getBytes(StandardCharsets.UTF_8))) {
                throw new GeneralException(AuthErrorCode.INVALID_APPLE_NONCE);
            }
            String email = null;
            Object verified = claims.get("email_verified");
            if (Boolean.TRUE.equals(verified) || "true".equals(verified)) {
                email = claims.get("email", String.class);
                if (email != null && (email.isBlank() || email.length() > 255)) {
                    throw new GeneralException(AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
                }
            }
            replayStore.consume(sha256(identityToken), claims.getExpiration().toInstant());
            return new AppleIdentityClaims(claims.getSubject(), email);
        } catch (ExpiredJwtException ex) {
            throw new GeneralException(AuthErrorCode.EXPIRED_APPLE_IDENTITY_TOKEN);
        } catch (JwtException | IllegalArgumentException ex) {
            throw new GeneralException(AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
        }
    }

    private void validateConfiguration() {
        if (!StringUtils.hasText(properties.issuer()) || properties.jwksUri() == null
                || !properties.jwksUri().isAbsolute() || properties.jwksUri().getHost() == null
                || !("https".equals(properties.jwksUri().getScheme())
                    || ("http".equals(properties.jwksUri().getScheme())
                        && java.util.Set.of("localhost", "127.0.0.1", "[::1]")
                            .contains(properties.jwksUri().getHost())))
                || properties.allowedAudiences() == null || properties.allowedAudiences().isEmpty()
                || properties.allowedAudiences().stream().anyMatch(audience -> !StringUtils.hasText(audience))) {
            // 미설정 환경에서도 기존 제공자 로그인은 동작하도록 Apple 요청 시에만 검사한다.
            throw new GeneralException(AuthErrorCode.MISSING_OAUTH_CONFIGURATION);
        }
    }

    private static String sha256(String rawNonce) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(rawNonce.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256을 사용할 수 없습니다.", ex);
        }
    }
}
