package com.example.jejugilmoa.domain.auth.verifier;

import com.example.jejugilmoa.domain.auth.client.AppleJwksClient;
import com.example.jejugilmoa.domain.auth.config.AppleAuthProperties;
import com.example.jejugilmoa.domain.auth.exception.AuthErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Date;
import java.util.Set;

import static com.example.jejugilmoa.domain.auth.support.AppleTokenFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppleJwtIdentityTokenVerifierTest {
    @Mock AppleJwksClient client;
    @Mock AppleIdentityTokenReplayStore replayStore;
    AppleJwtIdentityTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        verifier = new AppleJwtIdentityTokenVerifier(client, properties("https://appleid.apple.com/auth/keys"), replayStore);
        lenient().when(client.getKey(KID)).thenReturn(KEYS.getPublic());
    }

    @Test void acceptsValidTokenAndVerifiedEmail() {
        var result = verifier.verify(sign(token()), NONCE);
        assertThat(result.subject()).isEqualTo("apple-test-sub");
        assertThat(result.email()).isEqualTo("apple@example.com");
    }

    @Test void acceptsBooleanEmailVerificationAndArrayAudience() {
        var result = verifier.verify(sign(token().claim("email_verified", true)
                .audience().add("another-client").and()), NONCE);
        assertThat(result.email()).isEqualTo("apple@example.com");
    }

    @Test void omitsUnverifiedOrAbsentEmail() {
        assertThat(verifier.verify(sign(token().claim("email_verified", false)), NONCE).email()).isNull();
        assertThat(verifier.verify(sign(token().claim("email", null)), NONCE).email()).isNull();
    }

    @Test void rejectsWrongSignature() {
        String value = token().signWith(Jwts.SIG.RS256.keyPair().build().getPrivate(), Jwts.SIG.RS256).compact();
        rejects(value, AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
    }

    @Test void rejectsWrongIssuer() {
        rejects(sign(token().issuer("https://attacker.example")), AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
    }

    @Test void rejectsWrongAudience() {
        rejects(sign(token().audience().clear().add("other-app").and()), AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
    }

    @Test void rejectsExpiredToken() {
        rejects(sign(token().expiration(Date.from(Instant.now().minusSeconds(60)))),
                AuthErrorCode.EXPIRED_APPLE_IDENTITY_TOKEN);
    }

    @Test void rejectsMissingExpirationAndSubject() {
        rejects(sign(token().expiration(null)), AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
        rejects(sign(token().subject(null)), AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
        rejects(sign(token().subject("s".repeat(256))), AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
    }

    @Test void rejectsNonceMismatchAndMissingNonce() {
        rejects(sign(token().claim("nonce", "wrong")), AuthErrorCode.INVALID_APPLE_NONCE);
        rejects(sign(token().claim("nonce", null)), AuthErrorCode.INVALID_APPLE_NONCE);
    }

    @Test void rejectsMissingKidAndUnsupportedAlgorithmBeforeKeyLookup() {
        rejects(sign(token().header().keyId(null).and()), AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
        rejects(token().signWith(Jwts.SIG.HS256.key().build()).compact(), AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
        rejects(token().compact(), AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
        verifyNoInteractions(client);
    }

    @Test void rejectsMalformedToken() {
        rejects("not-a-jwt", AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
        verifyNoInteractions(client);
    }

    @Test void preservesJwksOutageAsProviderError() {
        when(client.getKey(KID)).thenThrow(new GeneralException(AuthErrorCode.OAUTH_PROVIDER_ERROR));
        rejects(sign(token()), AuthErrorCode.OAUTH_PROVIDER_ERROR);
    }

    @Test void rejectsMissingConfigurationWithoutJwksCall() {
        var base = properties("https://appleid.apple.com/auth/keys");
        verifier = new AppleJwtIdentityTokenVerifier(client,
                new AppleAuthProperties(base.issuer(), base.jwksUri(), Set.of()), replayStore);
        rejects(sign(token()), AuthErrorCode.MISSING_OAUTH_CONFIGURATION);
        verifyNoInteractions(client);
    }

    private void rejects(String value, AuthErrorCode code) {
        assertThatThrownBy(() -> verifier.verify(value, NONCE)).isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode()).isEqualTo(code);
        verifyNoInteractions(replayStore);
    }
}
