-- BadgeConditionRepository.findAllByBadgeIdIn 조회 패턴에 맞는 인덱스.
-- CONCURRENTLY는 트랜잭션 내에서 실행할 수 없어 CHECK 제약 백필(V2)과 분리했다.
-- 일반 CREATE INDEX는 테이블에 SHARE 락을 걸어 쓰기를 막지만, CONCURRENTLY는
-- 쓰기를 막지 않는 대신 반드시 트랜잭션 밖에서 실행되어야 한다.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_badge_condition_badge
    ON public.badge_condition USING btree (badge_id);
