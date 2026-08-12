-- ST_DWithin(geom::geography, ...) 기반 반경/코스 검색(findNearby 등)이 타는 인덱스.
-- findWithinBounds/findWithinBoundsAndCategory가 쓰는 geometry GiST 인덱스(idx_place_geom_published,
-- V1__baseline.sql)와는 별개이며 서로 대체하지 않는다 — 두 인덱스 모두 유지해야 한다.
-- CONCURRENTLY는 트랜잭션 내에서 실행할 수 없어 별도 마이그레이션 파일로 분리했다 (V3와 동일한 이유).
-- 운영 환경에서 CREATE INDEX(비-CONCURRENTLY)는 place 테이블에 SHARE 락을 걸어 빌드 내내 쓰기를
-- 막으므로, 반드시 CONCURRENTLY로 생성해 쓰기 차단을 피한다.
CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_place_geom_geography_published
    ON public.place USING gist ((geom::geography)) WHERE (is_published = true);
