-- Hibernate ddl-auto(dev=update, prod=validate)는 공간 인덱스를 생성하지 못하므로
-- (ADR-0002/0005, 마이그레이션 도구 미도입) 별도로 명시한다.
-- 매 기동마다 재실행되지만 IF NOT EXISTS라 이미 존재하면 그대로 스킵된다.
--
-- 지도 뷰포트 조회(PlaceRepository#findWithinBounds*)를 포함해 geom을 사용하는
-- 모든 쿼리가 is_published = true 조건을 함께 걸므로, 인덱스도 그 조건으로 좁혀 크기를 줄인다.
--
-- CONCURRENTLY 생성이 이전 기동에서 중단(크래시/취소)되면 postgres는 INVALID 상태의
-- 인덱스를 그대로 남긴다. 이후 기동은 "이름이 존재한다"는 이유로 IF NOT EXISTS에서
-- 스킵되어 영원히 재생성되지 않고, 공간 쿼리는 이 인덱스를 쓰지 못한 채 시퀀셜 스캔으로
-- 빠진다. 그래서 재생성 전에 invalid 인덱스를 먼저 정리한다.
-- (DROP INDEX CONCURRENTLY도 트랜잭션 밖에서만 가능해 DO 블록 안에서는 호출할 수 없으므로,
-- 여기서는 일반 DROP INDEX를 쓴다 — 어차피 invalid 인덱스는 플래너가 쓰지 않으므로
-- 짧은 잠금으로 지우는 비용은 무시할 만하다.)
DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_index i
        JOIN pg_class c ON c.oid = i.indexrelid
        WHERE c.relname = 'idx_place_geom_published'
          AND i.indisvalid = false
    ) THEN
        DROP INDEX IF EXISTS idx_place_geom_published;
    END IF;
END $$;

CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_place_geom_published
    ON place USING GIST (geom)
    WHERE is_published = true;
