package com.example.jejugilmoa.domain.auth.client;

import com.example.jejugilmoa.domain.auth.exception.AuthErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;

import static com.example.jejugilmoa.domain.auth.support.AppleTokenFixture.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

class AppleJwksClientTest {
    private static final String URI = "https://appleid.apple.com/auth/keys";
    MockRestServiceServer server;
    AppleJwksClient client;
    Clock clock;
    Instant now = Instant.parse("2026-01-01T00:00:00Z");

    @BeforeEach void setUp() {
        var builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        clock = mock(Clock.class);
        when(clock.instant()).thenReturn(now);
        client = new AppleJwksClient(builder.build(), properties(URI), clock);
    }

    @Test void cachesKeys() {
        server.expect(requestTo(URI)).andRespond(withSuccess(jwks(KID), MediaType.APPLICATION_JSON));
        assertThat(client.getKey(KID)).isEqualTo(KEYS.getPublic());
        assertThat(client.getKey(KID)).isEqualTo(KEYS.getPublic());
        server.verify();
    }

    @Test void refreshesUnknownKidAfterCooldownAndLimitsRepeatedMisses() {
        server.expect(requestTo(URI)).andRespond(withSuccess(jwks(KID), MediaType.APPLICATION_JSON));
        server.expect(requestTo(URI)).andRespond(withSuccess(jwks("rotated"), MediaType.APPLICATION_JSON));
        client.getKey(KID);
        rejects("missing", AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
        when(clock.instant()).thenReturn(now.plusSeconds(31));
        assertThat(client.getKey("rotated")).isEqualTo(KEYS.getPublic());
        rejects("missing-again", AuthErrorCode.INVALID_APPLE_IDENTITY_TOKEN);
        server.verify();
    }

    @Test void refreshesExpiredCache() {
        server.expect(requestTo(URI)).andRespond(withSuccess(jwks(KID), MediaType.APPLICATION_JSON));
        server.expect(requestTo(URI)).andRespond(withSuccess(jwks(KID), MediaType.APPLICATION_JSON));
        client.getKey(KID);
        when(clock.instant()).thenReturn(now.plusSeconds(3601));
        assertThat(client.getKey(KID)).isEqualTo(KEYS.getPublic());
        server.verify();
    }

    @Test void handlesOutageWithoutRepeatedFetch() {
        server.expect(requestTo(URI)).andRespond(withServerError());
        rejects(KID, AuthErrorCode.OAUTH_PROVIDER_ERROR);
        rejects(KID, AuthErrorCode.OAUTH_PROVIDER_ERROR);
        server.verify();
    }

    @Test void rejectsMalformedJwks() {
        server.expect(requestTo(URI)).andRespond(withSuccess("{bad-json", MediaType.APPLICATION_JSON));
        rejects(KID, AuthErrorCode.OAUTH_PROVIDER_ERROR);
        server.verify();
    }

    private void rejects(String kid, AuthErrorCode code) {
        assertThatThrownBy(() -> client.getKey(kid)).isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode()).isEqualTo(code);
    }
}
