# 0004. 테스트 전략과 Testcontainers 도입

- 상태: Proposed
- 날짜: 2026-07-07

## 배경 (Context)

PostGIS geometry 컬럼 때문에 H2 인메모리 DB를 쓸 수 없다. 현재 `@SpringBootTest`는
로컬 docker compose DB에 연결하며, DB가 없으면 `./gradlew test` 자체가 실패한다.
"체크아웃 → 테스트"가 한 번에 안 되는 상태는 사람과 AI 도구 모두의 검증 루프를 끊는다.

## 결정 (Decision)

1. (즉시 적용) 레이어별 전략: Service는 Mockito 단위 테스트, Controller는
   `@WebMvcTest`, DB 필수 테스트(공간 쿼리)만 실제 PostGIS 사용. [testing.md](../testing.md) 참고.
2. (제안) `spring-boot-testcontainers` + `postgis/postgis` 컨테이너를 도입해
   DB 필수 테스트가 docker compose 수동 기동 없이 스스로 DB를 띄우게 한다.

## 결과 (Consequences)

- 도입 시 `./gradlew test`가 단독으로 재현 가능해지고 CI의 서비스 컨테이너 설정도 제거 가능.
- 로컬에 Docker가 반드시 필요해진다 (현재도 사실상 필요하므로 추가 부담 아님).
- 의존성 추가(`spring-boot-testcontainers`, `org.testcontainers:postgresql`)가 필요하다.
  **빌드 변경이므로 팀 합의 후 Accepted로 전환하고 적용한다.**

## 고려한 대안 (Alternatives)

- H2 + geometry 흉내 — hibernate-spatial의 H2 지원은 PostGIS 함수와 호환되지 않음. 기각.
- 공간 필드/쿼리만 테스트에서 제외 — 핵심 기능(반경 검색)이 미검증 상태로 남음. 기각.
