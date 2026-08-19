-- token 컬럼의 일반 인덱스를 유니크 제약으로 교체한다.
-- V3/V11과 달리 CONCURRENTLY를 사용하지 않는다: DROP INDEX CONCURRENTLY는 Flyway mixed=true
-- 환경에서도 트랜잭션 내 실행으로 처리되어 PostgreSQL이 거부한다. device_token 테이블은
-- 신규 테이블이라 데이터가 적어 순간적인 잠금이 허용된다.

-- 1. 중복 token 데이터 정리: 동일 token이 여러 행 존재하면 가장 오래된 행만 남긴다.
DELETE FROM public.device_token
WHERE id NOT IN (
    SELECT MIN(id)
    FROM public.device_token
    GROUP BY token
);

-- 2. 유니크 인덱스를 생성한다.
CREATE UNIQUE INDEX IF NOT EXISTS uk_device_token_token
    ON public.device_token USING btree (token);

-- 3. 생성된 인덱스를 유니크 제약조건으로 연결한다.
ALTER TABLE public.device_token
    ADD CONSTRAINT uk_device_token_token UNIQUE USING INDEX uk_device_token_token;

-- 4. 기존 일반 인덱스를 제거한다.
DROP INDEX IF EXISTS idx_device_token_token;
