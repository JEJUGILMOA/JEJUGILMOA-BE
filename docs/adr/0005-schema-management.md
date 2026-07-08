# 0005. 스키마 관리: ddl-auto 정책과 마이그레이션 도구

- 상태: Proposed
- 날짜: 2026-07-07

## 배경 (Context)

현재 dev는 `ddl-auto: update`, prod는 `validate`다. update는 초기 개발 속도는 빠르지만:

- 컬럼 삭제/이름 변경을 반영하지 못한다 (누적된 쓰레기 컬럼 발생)
- PostGIS geom 컬럼의 GiST 공간 인덱스를 만들지 못한다 ([ADR-0002](0002-postgis-dual-storage.md))
- prod가 `validate`이므로 **운영 스키마를 만들/바꿀 수단이 현재 없다**

## 결정 (Decision)

1. (현행 유지) 엔티티가 안정화될 때까지 dev=`update`, prod=`validate`를 유지한다.
2. (제안) 첫 운영 배포 전에 Flyway를 도입한다:
   - `V1__baseline.sql`에 전체 스키마 + `CREATE EXTENSION postgis` + GiST 인덱스 명시
   - 도입 후 dev도 `ddl-auto: validate`로 전환
   - 이후 모든 스키마 변경은 마이그레이션 파일로만 수행

## 결과 (Consequences)

- 도입 전까지: 엔티티 변경 시 dev DB에 자동 반영되지만, **prod 반영 경로가 없음을
  인지하고 있어야 한다.** 운영 배포가 가시화되면 이 ADR을 우선 처리할 것.
- 도입 후: 스키마 이력이 코드로 남고, 리뷰 가능해진다. 대신 엔티티와 마이그레이션을
  같은 PR에서 함께 수정해야 하는 규율이 필요하다.

## 고려한 대안 (Alternatives)

- Liquibase — 기능은 대등하나 팀에 SQL 우선 방식(Flyway)이 더 익숙하고 단순. 보류.
- prod도 `update` — 운영 DB를 Hibernate가 임의 변경하는 것은 수용 불가. 기각.
