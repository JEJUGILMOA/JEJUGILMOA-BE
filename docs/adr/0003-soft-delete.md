# 0003. deletedAt 기반 소프트 삭제

- 상태: Accepted
- 날짜: 2026-07-07 (기존 코드에 구현된 결정을 소급 기록)

## 배경 (Context)

사용자 탈퇴와 여행 기록 삭제 후에도 신고 처리(`Report`가 `targetId`로 참조),
통계, 복구 요구가 남는다. 물리 삭제하면 다형 참조가 깨진다.

## 결정 (Decision)

- 복구/이력이 필요한 엔티티(`User`, `TravelRecord`)는 nullable `deletedAt`
  timestamp 컬럼으로 소프트 삭제한다. boolean 플래그가 아닌 timestamp를 쓰는 이유는
  "언제 삭제됐는가"가 운영·정책(보존 기간)상 필요하기 때문이다.
- 순수 자식 데이터(이미지, 코스 등)는 부모 cascade + `orphanRemoval`로 물리 삭제한다.

## 결과 (Consequences)

- **조회 필터 규칙**: 소프트 삭제 엔티티를 조회하는 모든 쿼리는 `deletedAt IS NULL`
  조건을 넣어야 한다. 누락이 반복되면 Hibernate `@SQLRestriction` 도입을 검토한다
  (관리자용 "삭제 포함 조회"가 필요해질 수 있어 아직 미적용).
- unique 제약과 충돌 주의: `User.externalId`가 unique이므로 탈퇴 후 동일 계정 재가입
  시나리오를 구현할 때 externalId 처리 방침(무효화 등)을 정해야 한다.

## 고려한 대안 (Alternatives)

- 물리 삭제 — 신고/통계 참조 무결성 깨짐. 기각.
- 삭제 이력 테이블 분리 — 조회는 깔끔하지만 초기 단계에 과한 복잡도. 기각.
