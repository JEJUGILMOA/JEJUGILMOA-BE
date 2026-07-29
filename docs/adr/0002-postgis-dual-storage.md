# 0002. 좌표 이중 저장 (lat/lng + PostGIS geometry)

- 상태: Accepted
- 날짜: 2026-07-07 (기존 코드에 구현된 결정을 소급 기록)

## 배경 (Context)

핵심 기능이 "내 주변 장소 반경 검색"이다. 일반 컬럼(lat/lng)에 대한 하버사인 계산은
인덱스를 못 타서 느리고, PostGIS의 `ST_DWithin` + 공간 인덱스가 표준 해법이다.
한편 API 응답과 일반 조회에서는 숫자 좌표가 그대로 필요하다.

## 결정 (Decision)

`Place`에 좌표를 **이중 저장**한다:

- `latitude`/`longitude` (`BigDecimal`, `precision 10/11, scale 8`) — API 응답·일반 조회용
- `geom` (`geometry(Point, 4326)`, JTS `Point`, `@JdbcTypeCode(SqlTypes.GEOMETRY)`) —
  `ST_DWithin` 반경 검색용. `ST_Point(longitude, latitude)` 순서로 생성 (경도 먼저!)

## 결과 (Consequences)

- **동기화 규칙(불변식)**: Place를 생성/수정하는 모든 코드는 lat/lng와 geom을 반드시
  같이 갱신해야 한다. 이 규칙은 서비스가 아닌 엔티티의 상태 변경 메서드에 캡슐화할 것.
- 로컬/CI DB는 반드시 PostGIS 이미지를 써야 한다 (일반 postgres, H2 불가).
- geom 컬럼에는 GiST 공간 인덱스가 필요하다 — ddl-auto는 공간 인덱스를 만들어주지 않으므로,
  마이그레이션 도구 도입 전까지는 `src/main/resources/schema.sql`이 기동 시마다
  `idx_place_geom_published`(부분 인덱스, `WHERE is_published = true`)를
  `CREATE INDEX CONCURRENTLY IF NOT EXISTS`로 멱등하게 생성한다 ([ADR-0005](0005-schema-management.md)
  도입 후에는 이 로직을 마이그레이션 파일로 옮길 것).

## 고려한 대안 (Alternatives)

- lat/lng만 저장 + 하버사인 쿼리 — 인덱스 불가, 데이터 증가 시 풀스캔. 기각.
- geom만 저장하고 응답 시 좌표 추출 — 조회마다 `ST_X/ST_Y` 호출 필요, DTO 매핑 번잡. 기각.
