# 테스트 전략

## 현재 상태와 제약

- 현재 테스트: `JejugilmoaApplicationTests`(contextLoads), `AuthControllerTest`, `AuthServiceTest`,
  `PlacePersistServiceTest`, `PlaceSyncServiceTest`. Service 단위 테스트(`PlaceSyncServiceTest` 등)는
  아래 레이어별 전략대로 Mockito로 목킹해 DB 없이 돈다.
- **`@SpringBootTest`는 실제 DB에 연결한다** (dev 프로필 기본값 = localhost:5432).
  따라서 `./gradlew test`는 `docker compose up -d` 이후에만 통과한다. CI도 PostGIS
  서비스 컨테이너를 띄워 동일하게 동작한다 (`.github/workflows/ci.yml`).
- **H2 등 인메모리 DB는 쓸 수 없다** — `Place.geom`(PostGIS geometry)과 네이티브
  공간 쿼리 때문. 이 제약이 테스트 전략 전체를 결정한다.

## Boot 4 테스트 스타터 주의

Spring Boot 4는 테스트 스타터가 분리됐다. 이 프로젝트가 쓰는 것:

- `spring-boot-starter-webmvc-test` → `@WebMvcTest`, `MockMvc`
- `spring-boot-starter-data-jpa-test` → `@DataJpaTest`
- `spring-security-test` → 시큐리티 목킹

`spring-boot-starter-test`(3.x식 통합 스타터)를 추가하지 말 것.

## 레이어별 전략

| 대상 | 방법 | DB 필요 |
|---|---|---|
| Service 로직 | 순수 단위 테스트 + Mockito로 repository 목킹 | ❌ |
| Controller + 응답 봉투 | `@WebMvcTest` + service 목킹. `GeneralExceptionAdvice` 포함해 에러 봉투 형식까지 검증 | ❌ |
| Repository (특히 공간 쿼리) | `@DataJpaTest(properties = "spring.test.database.replace=none")` + 실제 PostGIS | ✅ |
| 전체 컨텍스트 | `@SpringBootTest` — 최소한으로 유지 | ✅ |

**우선순위**: 빠르고 DB 없는 테스트(Service 단위, `@WebMvcTest`)를 기본으로 하고,
DB가 필요한 테스트는 공간 쿼리처럼 실제 DB 없이는 검증 불가능한 것에만 쓴다.

## 실행

```bash
docker compose up -d                     # DB 필요 테스트 전 필수
./gradlew test                           # 전체
./gradlew test --tests "com.example.jejugilmoa.domain.plan.*"   # 패키지 단위
./gradlew test --tests "*.TravelPlanServiceTest"                # 클래스 단위
```

실패 리포트: `build/reports/tests/test/index.html`

## 향후 과제 (제안 상태 — [ADR-0004](adr/0004-testing-strategy.md))

- **Testcontainers 도입**: 현재는 개발자가 docker compose를 수동으로 띄워야 테스트가
  돈다. `spring-boot-testcontainers` + `postgis/postgis` 컨테이너로 전환하면 `./gradlew
  test` 단독으로 재현 가능해진다. 의존성 추가가 필요하므로 팀 합의 후 진행.
