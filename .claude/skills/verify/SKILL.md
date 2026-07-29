---
name: verify
description: Verify a code change end-to-end in this Spring Boot app — start the PostGIS DB, run tests, boot the app, and exercise the changed endpoint through real HTTP. Use before committing any nontrivial change to src/main.
---

# Verify a change end-to-end

Tests alone are not sufficient verification here: the only existing test is a context load,
and API behavior (response envelope shape, error codes, validation messages) must be
observed over real HTTP.

## 1. Database up (prerequisite for everything)

```bash
docker compose up -d
docker compose ps   # wait until db is healthy
```

Without this, even `./gradlew test` fails — `@SpringBootTest` connects to a real DB.

## 2. Build + tests

```bash
./gradlew build
```

If tests fail, read `build/reports/tests/test/index.html` (or the console stack trace).
Do not proceed to runtime verification with a red build.

## 3. Boot the app

```bash
./gradlew bootRun
```

Run in background; the app is ready when the log shows `Started JejugilmoaApplication`.
Readiness probe:

```bash
curl -s http://localhost:8080/health   # expect: ok
```

## 4. Exercise the actual change

- **New/changed endpoint**: call it with curl. Verify BOTH the happy path and at least one
  failure path (bad input, missing resource). Check the envelope:
  - success → `{"isSuccess":true,"code":"COMMON200",...,"result":...}`
  - failure → `{"isSuccess":false,"code":"<DOMAIN><STATUS>_<N>",...}` with the correct HTTP status
- **Entity/schema change**: dev profile is `ddl-auto: update` — confirm the schema Hibernate
  produced:
  ```bash
  docker compose exec db psql -U postgres -d jejugilmoa -c "\d <table_name>"
  ```
- **Swagger**: `curl -s http://localhost:8080/v3/api-docs` should include the new path.

## 5. Report honestly

State exactly what was exercised and what the responses were. Most routes require a valid
JWT by default (`SecurityConfig`, ADR-0006) — if you couldn't obtain one and a path needs
auth, say so explicitly instead of implying full coverage, rather than skipping silently.
