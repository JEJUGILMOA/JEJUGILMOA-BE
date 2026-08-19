-- token 컬럼의 일반 인덱스를 유니크 제약으로 교체한다.
-- CONCURRENTLY는 트랜잭션 밖에서 실행되어야 하므로 spring.flyway.mixed=true 필요.
-- (spring.flyway.postgresql.transactional-lock=false 도 적용되어 있음)

-- 1. 중복 token 데이터 정리: 동일 token이 여러 행 존재하면 가장 오래된 행만 남긴다.
DELETE FROM public.device_token
WHERE id NOT IN (
    SELECT MIN(id)
    FROM public.device_token
    GROUP BY token
);

-- 2. 유니크 인덱스를 먼저 논블로킹으로 생성한다.
CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS uk_device_token_token
    ON public.device_token USING btree (token);

-- 3. 생성된 인덱스를 유니크 제약조건으로 연결한다 (테이블 락 없음).
ALTER TABLE public.device_token
    ADD CONSTRAINT uk_device_token_token UNIQUE USING INDEX uk_device_token_token;

-- 4. 기존 일반 인덱스를 논블로킹으로 제거한다.
DROP INDEX CONCURRENTLY IF EXISTS idx_device_token_token;
