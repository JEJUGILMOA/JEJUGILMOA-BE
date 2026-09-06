package com.example.jejugilmoa.domain.auth.client;

import com.example.jejugilmoa.domain.auth.config.AppleAuthProperties;
import com.example.jejugilmoa.domain.auth.dto.AppleJwksResponse;
import com.example.jejugilmoa.domain.auth.exception.AuthErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Component
public class AppleJwksClient {
    private static final Duration CACHE_TTL = Duration.ofHours(1);
    private static final Duration REFRESH_INTERVAL = Duration.ofSeconds(30);
    private final RestClient restClient;
    private final AppleAuthProperties properties;
    private final Clock clock;
    private Map<String, PublicKey> keys = Map.of();
    private Instant expiresAt = Instant.MIN;
    private Instant nextRefreshAt = Instant.MIN;
    private boolean lastRefreshFailed;

    @org.springframework.beans.factory.annotation.Autowired
    public AppleJwksClient(@Qualifier("appleJwksRestClient") RestClient restClient, AppleAuthProperties properties) {
        this(restClient, properties, Clock.systemUTC());
    }

    AppleJwksClient(RestClient restClient, AppleAuthProperties properties, Clock clock) {
        this.restClient = restClient;
        this.properties = properties;
        this.clock = clock;
    }

    // 키 교체 시 재조회하되 임의 kid 요청으로 외부 호출이 폭증하지 않도록 전체 갱신을 제한한다.
    public synchronized PublicKey getKey(String kid) {
        Instant now = clock.instant();
        if (now.isBefore(expiresAt) && keys.containsKey(kid)) {
            return keys.get(kid);
        }
        if (!now.isBefore(nextRefreshAt)) {
            nextRefreshAt = now.plus(REFRESH_INTERVAL);
            refresh(now);
        }
        if (lastRefreshFailed) {
            throw new GeneralException(AuthErrorCode.OAUTH_PROVIDER_ERROR);
        }
        PublicKey key = now.isBefore(expiresAt) ? keys.get(kid) : null;
        if (key == null) {
            throw new GeneralException(AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
        }
        return key;
    }

    private void refresh(Instant now) {
        try {
            AppleJwksResponse response = restClient.get().uri(properties.jwksUri())
                    .retrieve().body(AppleJwksResponse.class);
            if (response == null || response.keys() == null || response.keys().isEmpty()
                    || response.keys().size() > 100) {
                throw new IllegalArgumentException("Apple 공개키 응답 형식 오류");
            }
            Map<String, PublicKey> refreshed = new HashMap<>();
            for (var key : response.keys()) {
                if (key == null) {
                    throw new IllegalArgumentException("Apple 공개키 누락");
                }
                if (!"RSA".equals(key.kty()) || !"RS256".equals(key.alg())
                        || (key.use() != null && !"sig".equals(key.use()))) {
                    continue;
                }
                if (key.kid() == null || key.kid().isBlank() || key.kid().length() > 256
                        || key.n() == null || key.e() == null || key.n().length() > 2048 || key.e().length() > 16) {
                    throw new IllegalArgumentException("Apple 공개키 필드 오류");
                }
                var spec = new RSAPublicKeySpec(
                        new BigInteger(1, Base64.getUrlDecoder().decode(key.n())),
                        new BigInteger(1, Base64.getUrlDecoder().decode(key.e())));
                var publicKey = (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(spec);
                if (publicKey.getModulus().bitLength() < 2048
                        || refreshed.putIfAbsent(key.kid(), publicKey) != null) {
                    throw new IllegalArgumentException("Apple 공개키 강도 또는 식별자 오류");
                }
            }
            if (refreshed.isEmpty()) {
                throw new IllegalArgumentException("Apple 서명용 공개키 누락");
            }
            keys = Map.copyOf(refreshed);
            expiresAt = now.plus(CACHE_TTL);
            lastRefreshFailed = false;
        } catch (RestClientException | GeneralSecurityException | IllegalArgumentException ex) {
            lastRefreshFailed = true;
            throw new GeneralException(AuthErrorCode.OAUTH_PROVIDER_ERROR);
        }
    }
}
