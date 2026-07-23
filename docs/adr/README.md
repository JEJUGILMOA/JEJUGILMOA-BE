# ADR (Architecture Decision Records)

되돌리기 어렵거나 앞으로의 코드 작성 방식을 구속하는 결정을 기록한다.
코드는 "무엇을" 보여주지만 "왜"는 보여주지 못한다 — 그 간극을 여기서 메운다.

## 작성 규칙

1. [template.md](template.md)를 복사해 `NNNN-케밥-제목.md`로 저장 (번호는 순차 증가)
2. 상태: `Proposed`(제안) → `Accepted`(채택) → 필요 시 `Superseded by NNNN`(대체됨)
3. 결정을 **번복할 때는 기존 ADR을 수정하지 말고** 새 ADR을 쓰고 기존 것을 Superseded 처리
4. PR에 아키텍처 결정이 포함되면 ADR을 같은 PR에 포함시킨다

## 목록

| 번호 | 제목 | 상태 |
|---|---|---|
| [0001](0001-api-response-envelope.md) | API 응답 봉투와 중앙집중 예외 처리 | Accepted |
| [0002](0002-postgis-dual-storage.md) | 좌표 이중 저장 (lat/lng + PostGIS geometry) | Accepted |
| [0003](0003-soft-delete.md) | deletedAt 기반 소프트 삭제 | Accepted |
| [0004](0004-testing-strategy.md) | 테스트 전략과 Testcontainers 도입 | Proposed |
| [0005](0005-schema-management.md) | 스키마 관리: ddl-auto 정책과 마이그레이션 도구 | Proposed |
| [0006](0006-jwt-cookie-auth.md) | JWT 기반 쿠키 인증과 리프레시 토큰 회전 | Accepted |
| [0007](0007-place-api-pipeline.md) | Place 데이터 파이프라인 및 탐색 API 구현 | 구현 완료 |
