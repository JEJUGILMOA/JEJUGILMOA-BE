# 제주길모아 백엔드 (JEJUGILMOA-BE)

제주 여행 계획·추천 서비스의 백엔드 API 서버입니다.
사용자가 장소(Place)들을 엮어 여행 계획(TravelPlan)과 코스(TravelCourse)를 만들고,
완료한 여행을 기록(TravelRecord)으로 남겨 공유하고, 배지(Badge)를 획득하는 서비스입니다.

## 기술 스택

| 구분 | 기술 |
|---|---|
| Language | Java 21 |
| Framework | Spring Boot 4.1.0 (webmvc, data-jpa, security, validation) |
| Database | PostgreSQL + PostGIS (공간 검색: `ST_DWithin`) |
| ORM | Hibernate + hibernate-spatial + JTS |
| API 문서 | springdoc-openapi (Swagger UI) |
| Build | Gradle 9.5.1 (wrapper) |

> ⚠️ **Spring Boot 4.x**입니다. 스타터 이름이 3.x와 다릅니다
> (`spring-boot-starter-webmvc`, `spring-boot-starter-data-jpa-test` 등). 의존성 추가 시 주의하세요.

## 시작하기

### 요구사항

- JDK 21
- Docker (로컬 DB용)

### 1. DB 기동

PostGIS가 필요하므로 반드시 docker compose로 띄웁니다 (일반 postgres 불가):

```bash
docker compose up -d
```

### 2. 환경 변수

`.env.example`을 참고하세요. **dev 프로필은 모든 값에 기본값이 있어** docker compose 기본 설정 그대로면 별도 설정 없이 실행됩니다.

> **포트 5432가 이미 사용 중이라면** (다른 프로젝트의 postgres 등):
> 프로젝트 루트에 `.env` 파일을 만들고 `DB_PORT=5433`을 넣으세요. docker compose는
> `.env`를 자동으로 읽지만, **Spring Boot는 `.env`를 읽지 않으므로** 앱 실행 시에는
> 환경 변수로 직접 넘겨야 합니다: `DB_PORT=5433 ./gradlew bootRun` (또는 IntelliJ 실행
> 구성의 Environment variables에 등록).

### 3. 실행

```bash
./gradlew bootRun        # Windows: gradlew.bat bootRun
```

- Health check: http://localhost:8080/health
- Swagger UI: http://localhost:8080/swagger-ui.html

### 4. 테스트 / 빌드

```bash
./gradlew test           # ⚠️ @SpringBootTest가 DB에 연결하므로 docker compose up 이후 실행해야 함
./gradlew build
```

## 프로필

| 프로필 | ddl-auto | SQL 로깅 | 용도 |
|---|---|---|---|
| `dev` (기본) | `update` | on | 로컬 개발. 스키마 자동 반영 |
| `prod` | `validate` | off | 운영. 모든 DB 환경 변수 필수 |

## 문서

| 문서 | 내용 |
|---|---|
| [docs/architecture.md](docs/architecture.md) | 레이어 구조, 패키지 규칙, 도메인 모델 |
| [docs/conventions.md](docs/conventions.md) | 코드/커밋/브랜치 컨벤션 |
| [docs/testing.md](docs/testing.md) | 테스트 전략 |
| [docs/adr/](docs/adr/) | 아키텍처 결정 기록 (왜 이렇게 만들었는가) |
| [CLAUDE.md](CLAUDE.md) | Claude Code (AI 협업 도구) 안내 문서 |

## 커밋 컨벤션

```
[Feat] 여행 계획 생성 API 구현
[Fix] 프로젝트 기본 엔티티 수정
```

접두사: `[Feat]` `[Fix]` `[Refactor]` `[Test]` `[Docs]` `[Chore]` — 자세한 내용은 [docs/conventions.md](docs/conventions.md).
