-- Hibernate ddl-auto(dev=update, prod=validate)는 공간 인덱스를 생성하지 못하므로
-- (ADR-0002/0005, 마이그레이션 도구 미도입) 별도로 명시한다.
-- 매 기동마다 재실행되지만 IF NOT EXISTS라 이미 존재하면 그대로 스킵된다.
--
-- 지도 뷰포트 조회(PlaceRepository#findWithinBounds*)를 포함해 geom을 사용하는
-- 모든 쿼리가 is_published = true 조건을 함께 걸므로, 인덱스도 그 조건으로 좁혀 크기를 줄인다.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_place_geom_published
    ON place USING GIST (geom)
    WHERE is_published = true;
