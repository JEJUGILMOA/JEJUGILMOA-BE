# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

제주길모아 (Jejugilmoa) — Spring Boot backend for a Jeju travel-planning service. Users compose travel plans (`TravelPlan`) from ordered course stops (`TravelCourse`) through places (`Place`), record finished trips (`TravelRecord`), react/share, earn badges, and report content. Early stage: the full JPA domain model exists, but there are almost no repositories/services/controllers yet (only `HealthController`).

## Commands

```bash
docker compose up -d     # REQUIRED FIRST — PostGIS DB. Even `test` fails without it.
./gradlew build          # compile + tests (Windows shells: gradlew.bat)
./gradlew test --tests "*.SomeTest"   # single test class
./gradlew bootRun        # run locally (dev profile by default)
```

- Health: `curl http://localhost:8080/health` → `ok`. Swagger UI: `/swagger-ui.html`.
- Env vars: see `.env.example`; dev profile has working defaults for everything.
- CI (`.github/workflows/ci.yml`) mirrors this: build+test against a PostGIS service container.

## Critical gotchas

- **Spring Boot 4.x** (not 3.x): starter names differ — `spring-boot-starter-webmvc`, `spring-boot-starter-webmvc-test`, `spring-boot-starter-data-jpa-test`. Do not add `spring-boot-starter-test` or `spring-boot-starter-web`.
- **PostGIS is mandatory** — `Place.geom` is `geometry(Point,4326)`. Plain postgres or H2 will not boot the schema. Use `docker compose`, never suggest H2 for tests.
- **Dual coordinate storage**: `Place` keeps `latitude`/`longitude` (BigDecimal) AND `geom` — any code touching one must update both, and `ST_MakePoint` takes longitude first ([ADR-0002](docs/adr/0002-postgis-dual-storage.md)).
- **Soft delete**: `User` and `TravelRecord` use `deletedAt`; every query on them must filter `deletedAt IS NULL` ([ADR-0003](docs/adr/0003-soft-delete.md)).
- **Security is wide open**: `SecurityConfig` permits all requests; no auth/JWT exists yet despite Swagger's bearer config. Where a userId is needed, leave a `TODO` — don't invent a security context.
- **`ddl-auto: update`** (dev) never drops/renames columns — schema drift is fixed by `docker compose down -v`. Prod is `validate` and there is currently no migration path ([ADR-0005](docs/adr/0005-schema-management.md)).
- `RedisConfig` is an empty stub — Redis is not actually configured.

## Conventions (musts)

- **All API responses** use the `ApiResponse<T>` envelope; business failures are `throw new GeneralException(<BaseCode enum>)` handled centrally by `GeneralExceptionAdvice` — never try-catch in controllers, never bare `ResponseEntity`. Codes follow `<DOMAIN><HTTP_STATUS>_<N>` (e.g. `PLAN404_1`) with Korean messages.
- **Entities**: `@Getter @Builder @NoArgsConstructor(PROTECTED) @AllArgsConstructor(PRIVATE)`, extend `BaseEntity`, no setters (mutate via intention-revealing methods like `Report.approve()`), `@Enumerated(STRING)`, LAZY relations, explicit `@Table(indexes=...)`.
- **Layering**: Controller → Service → Repository; DTOs (Java records) at the API boundary, entities never leave services. Package layout per domain: `controller/service/repository/dto/converter/entity/enums/exception`.
- **Commits**: `[Feat]/[Fix]/[Refactor]/[Test]/[Docs]/[Chore] + 한국어 요약` (e.g. `[Feat] 여행 계획 생성 API 구현`). Comments and API messages are in Korean.

## Where the depth lives

| Doc | Content |
|---|---|
| [docs/architecture.md](docs/architecture.md) | Layering rules, package structure, **ERD** (update it when entities change), decision summary |
| [docs/conventions.md](docs/conventions.md) | Naming, code style, commit/branch/PR rules |
| [docs/testing.md](docs/testing.md) | Test strategy per layer, Boot 4 test-starter pitfalls |
| [docs/adr/](docs/adr/README.md) | Why decisions were made; write a new ADR when making architectural choices |

## Project skills

- `verify` — end-to-end check of a change: DB up → build → bootRun → curl happy + failure paths. Use before committing changes to `src/main`.
- `new-feature` — scaffold a full API vertical slice with correct templates for every layer.
- `new-entity` — entity checklist (auditing, indexes, soft delete, spatial fields, ERD update).
- `error-codes` — add `BaseCode` enums correctly (domain vs general codes).
- `db` — local PostGIS operations: reset, psql inspection, spatial-query debugging.
