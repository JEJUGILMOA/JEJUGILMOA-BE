package com.example.jejugilmoa.domain.auth.verifier;

import com.example.jejugilmoa.domain.auth.exception.AuthErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AppleIdentityTokenReplayStoreTest {
    final StringRedisTemplate redis = mock(StringRedisTemplate.class);
    final AppleIdentityTokenReplayStore store = new AppleIdentityTokenReplayStore(redis);
    final Instant expiration = Instant.now().plusSeconds(60);

    @Test void rejectsReplay() {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(0L);
        rejects(AuthErrorCode.APPLE_IDENTITY_TOKEN_REUSED);
    }

    @Test void rejectsExpiredToken() {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class))).thenReturn(-1L);
        rejects(AuthErrorCode.EXPIRED_APPLE_IDENTITY_TOKEN);
    }

    @Test void failsClosedOnRedisOutage() {
        when(redis.execute(any(RedisScript.class), anyList(), any(Object[].class)))
                .thenThrow(new DataAccessResourceFailureException("연결 실패"));
        rejects(AuthErrorCode.APPLE_REPLAY_STORE_UNAVAILABLE);
    }

    @Test void failsClosedOnMissingResult() {
        rejects(AuthErrorCode.APPLE_REPLAY_STORE_UNAVAILABLE);
    }

    private void rejects(AuthErrorCode code) {
        assertThatThrownBy(() -> store.consume("digest", expiration))
                .isInstanceOf(GeneralException.class)
                .extracting(ex -> ((GeneralException) ex).getCode()).isEqualTo(code);
    }
}
