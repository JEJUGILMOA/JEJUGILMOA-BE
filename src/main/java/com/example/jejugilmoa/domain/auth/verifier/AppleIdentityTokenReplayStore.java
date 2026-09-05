package com.example.jejugilmoa.domain.auth.verifier;

import com.example.jejugilmoa.domain.auth.exception.AuthErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AppleIdentityTokenReplayStore {
    private final StringRedisTemplate redis;

    // 만료 확인과 최초 등록을 하나의 명령으로 실행한다. 원문 토큰/nonce는 저장하지 않는다.
    private static final DefaultRedisScript<Long> CONSUME = new DefaultRedisScript<>("""
            local now = redis.call('TIME')
            local nowMillis = now[1] * 1000 + math.floor(now[2] / 1000)
            if tonumber(ARGV[1]) <= nowMillis then
                return -1
            end
            if redis.call('SET', KEYS[1], '1', 'NX', 'PXAT', ARGV[1]) then
                return 1
            end
            return 0
            """, Long.class);

    public void consume(String digest, Instant expiresAt) {
        Long result;
        try {
            result = redis.execute(CONSUME, List.of("auth:apple:identity-token:" + digest),
                    Long.toString(expiresAt.toEpochMilli()));
        } catch (DataAccessException ex) {
            // 저장 실패 시 인증을 허용하지 않으며, 요청 원문이 포함될 수 있는 예외를 기록하지 않는다.
            throw new GeneralException(AuthErrorCode.APPLE_REPLAY_STORE_UNAVAILABLE);
        }
        if (Long.valueOf(-1).equals(result)) {
            throw new GeneralException(AuthErrorCode.EXPIRED_APPLE_IDENTITY_TOKEN);
        }
        if (Long.valueOf(0).equals(result)) {
            throw new GeneralException(AuthErrorCode.APPLE_IDENTITY_TOKEN_REUSED);
        }
        if (!Long.valueOf(1).equals(result)) {
            throw new GeneralException(AuthErrorCode.APPLE_REPLAY_STORE_UNAVAILABLE);
        }
    }
}
