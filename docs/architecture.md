# 아키텍처

이 문서는 레이어 구조와 패키지 규칙의 **단일 기준**이다.
새 코드는 (사람이 작성하든 AI가 작성하든) 이 문서의 규칙을 따라야 하며,
규칙을 바꾸는 결정은 [ADR](adr/)로 기록한 뒤 이 문서를 갱신한다.

## 레이어 구조

```
Controller  →  Service  →  Repository  →  DB
   ↓              ↓
  DTO          Entity
```

**의존 방향은 위에서 아래로만.** Repository가 Service를, Service가 Controller를 참조하면 안 된다.
Entity는 Controller 밖으로 나가지 않는다 — API 경계에서는 항상 DTO를 사용한다.

| 레이어 | 책임 | 규칙 |
|---|---|---|
| Controller | HTTP 매핑, 입력 검증(`@Valid`), 응답 봉투 | 비즈니스 로직 금지. `ApiResponse.onSuccess(...)`만 반환 |
| Service | 비즈니스 로직, 트랜잭션 경계 | 클래스에 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional` |
| Repository | 데이터 접근 | `JpaRepository` 상속. 공간 검색은 네이티브 쿼리(`ST_DWithin`) |
| Entity | 도메인 상태 + 상태 변경 메서드 | setter 금지. `Report.approve()`처럼 의도가 드러나는 메서드로 변경 |

## 패키지 구조 (도메인별 수직 슬라이스)

기능은 기술 레이어가 아니라 **도메인 기준**으로 묶는다. 도메인 하나의 완성형:

```
domain/plan/
  controller/   TravelPlanController
  service/      TravelPlanService            (커맨드/쿼리가 커지면 CommandService/QueryService 분리)
  repository/   TravelPlanRepository
  dto/          TravelPlanRequest, TravelPlanResponse   (Java record)
  converter/    TravelPlanConverter          (entity ↔ dto 변환, static 메서드)
  entity/       TravelPlan, TravelCourse, ...
  enums/        TravelPlanStatus, ...
  exception/    PlanErrorCode                (BaseCode 구현 enum)
```

현재는 `entity`/`enums`만 존재한다. 나머지 레이어를 추가할 때 이 구조를 따른다.
공통(횡단) 관심사는 `global/`에만 둔다: 응답 봉투(`apiPayload`), 설정(`config`), `BaseEntity`.

## API 응답 규칙

모든 응답은 `ApiResponse<T>` 봉투를 사용한다: `{isSuccess, code, message, result}`.

- 성공: `ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, result)`
- 비즈니스 실패: `throw new GeneralException(PlanErrorCode.PLAN_NOT_FOUND)` →
  `GeneralExceptionAdvice`가 봉투로 변환한다. **컨트롤러에서 try-catch 하지 않는다.**
- 프레임워크 예외(검증 실패, 타입 불일치, DB 제약 위반 등)도 `GeneralExceptionAdvice`가
  일괄 처리한다. 새 예외 유형은 컨트롤러가 아니라 advice에 핸들러를 추가한다.

에러 코드 체계는 [ADR-0001](adr/0001-api-response-envelope.md) 참고.

## 도메인 모델 (ERD)

> ⚠️ 엔티티를 추가/변경하면 이 다이어그램을 갱신할 것. (PR 체크리스트 항목)

```mermaid
erDiagram
    USER ||--o| USER_PREFERENCE : has
    USER ||--o| NOTIFICATION_SETTING : has
    USER ||--o{ USER_BADGE : earns
    USER ||--o{ USER_RESTRICTION : "restricted by"
    BADGE ||--o{ USER_BADGE : "granted as"
    BADGE ||--o{ BADGE_CONDITION : "unlocked by"
    BADGE_CONDITION ||--o{ BADGE_CONDITION_COURSE_STOP : "ordered course stops"
    PLACE ||--o{ BADGE_CONDITION_COURSE_STOP : "step of"
    BADGE_CONDITION {
        string condition_type "PLACE/CATEGORY/REGION/TOTAL_PLACES/CATEGORY_DIVERSITY/TRIP_COUNT/COURSE/ALL_BADGES"
        time time_start "시간조건형(일출/일몰/별빛) 인증 시간대; start>end면 자정 넘김"
        time time_end
    }

    USER ||--o{ TRAVEL_PLAN : creates
    TRAVEL_PLAN ||--o{ TRAVEL_COURSE : "ordered stops"
    TRAVEL_PLAN ||--o{ TRAVEL_PLAN_ROUTE : "daily saved driving routes"
    TRAVEL_PLAN_ROUTE {
        bigint id PK
        bigint plan_id FK
        date route_date "UNIQUE plan_id + route_date"
        jsonb path "longitude latitude arrays"
        integer total_distance "meter"
        bigint total_duration "millisecond"
        varchar route_option "traoptimal"
        varchar route_hash "SHA-256"
        varchar status "CALCULATING READY FAILED UNSUPPORTED NOT_REQUIRED"
        varchar failure_code
        timestamptz calculated_at
        timestamptz created_at
        timestamptz updated_at
    }
    TRAVEL_PLAN ||--o{ TRAVEL_PLAN_PREFERENCE : "styled by"
    TRAVEL_PLAN ||--o| TRAVEL_SHARED_PLAN : "shared as current plan"
    PLACE ||--o{ TRAVEL_COURSE : "visited in"
    USER ||--o{ FAVORITE : saves
    PLACE ||--o{ FAVORITE : "saved as"

    CATEGORY ||--o{ PLACE : classifies
    PLACE ||--o{ PLACE_IMAGE : gallery
    PLACE ||--o{ PLACE_CONGESTION : "congestion data"
    PLACE ||--o| POPULAR_PLACE : "ranked as"

    USER ||--o{ TRAVEL_RECORD : writes
    TRAVEL_PLAN |o--o| TRAVEL_RECORD : "recorded from; retained after plan deletion"
    TRAVEL_RECORD ||--o{ TRAVEL_RECORD_IMAGE : photos
    TRAVEL_RECORD o|--o| TRAVEL_RECORD_IMAGE : thumbnail
    TRAVEL_RECORD_PLACE o|--o{ TRAVEL_RECORD_IMAGE : "optional place photos"
    TRAVEL_RECORD ||--o{ TRAVEL_RECORD_PLACE : "visited places"
    TRAVEL_RECORD ||--o{ TRAVEL_RECORD_REACTION : "liked/disliked"
    TRAVEL_RECORD ||--o{ TRAVEL_SHARED_RECORD : "shared as"

    RECOMMENDED_COURSE ||--o{ RECOMMENDED_COURSE_PATH : "path of"

    REPORT }o--|| USER : "filed by (reporter_id)"

    USER ||--o{ REFRESH_TOKEN : "issued to (user_id)"

    USER ||--o{ NOTIFICATION : receives
    USER ||--o{ NOTIFICATION_READ : "read by (user_id)"
    USER ||--o{ DEVICE_TOKEN : registers
    NOTIFICATION ||--o{ NOTIFICATION_READ : "marked read as"
    NOTIFICATION {
        string type "PLAN_START/RECORD_WRITING/BADGE_ACQUIRED/NEXT_PLACE/PLACE_ARRIVAL/MARKETING"
        string data "딥링크 등 클라이언트 페이로드(JSON)"
        timestamp deleted_at "소프트 삭제(스와이프 삭제)"
    }

    LOCATION_USAGE_LOG {
        bigint subject_id "User PK value; no FK"
        string acquisition_path
        string service_code
        string recipient
        timestamptz received_at
    }
```

`Report`는 FK 없이 `targetType(RECORD/PHOTO/USER) + targetId`로 다형 참조한다.
`LocationUsageLog.subjectId`도 사용자 탈퇴 후 보존을 위해 User 연관관계와 FK 없이 PK 값만 저장한다.

## 주요 설계 결정 (요약 — 상세는 ADR)

| 결정 | 내용 | ADR |
|---|---|---|
| 응답 봉투 | `ApiResponse` + `BaseCode` enum 체계 | [0001](adr/0001-api-response-envelope.md) |
| 공간 데이터 | `Place`에 lat/lng(BigDecimal)와 PostGIS `geom` **이중 저장** — 항상 같이 갱신 | [0002](adr/0002-postgis-dual-storage.md) |
| 소프트 삭제 | `deletedAt` timestamp (User, TravelRecord, Notification) — 조회 시 필터 필수 | [0003](adr/0003-soft-delete.md) |
| 여행 기록 보존 | 계획과 기록의 lifecycle을 분리하고, 계획 삭제 시 `ON DELETE SET NULL`로 기록 snapshot 보존 | [0010](adr/0010-retain-travel-record-after-plan-deletion.md) |
| 데이터베이스 스키마 | Flyway를 통해 관리하며, 마이그레이션 파일은 `src/main/resources/db/migration` 경로에 버전 순서대로 추가한다. | — |
| 인증 | 외부 OAuth 로그인 + 앱 자체 JWT(액세스/리프레시, HttpOnly 쿠키). 리프레시 토큰은 DB 저장, 재발급 시 회전 + 재사용 탐지. **인가는 여전히 미구현 — 전 요청 permitAll** | [0006](adr/0006-jwt-cookie-auth.md) |
| 인증 | Apple 네이티브 로그인은 전용 verifier로 검증한 뒤 기존 회원 처리와 JWT/쿠키 발급을 사용 | [0011](adr/0011-native-apple-login.md) |

### 날짜별 계획 경로

`TravelCourse`를 그대로 유지하고 `TravelPlanRoute`에 날짜별 Directions 결과를 저장한다.
계획 생성·전체 수정·경유지 변경 트랜잭션은 최종 상태 저장 후 `PlanRouteChanged`를 발행한다.
커밋 후 동기 리스너가 짧은 별도 트랜잭션에서 입력을 준비하고, 트랜잭션 밖에서 Directions를 호출한다.
결과 저장 시 계획 잠금과 현재 입력 hash 재검증으로 오래된 결과 반영을 차단한다.
정책·장애 및 재시도 제약은 [ADR-0012](adr/0012-travel-plan-route.md), API는 [계획 경로](plan-routes.md)를 참고한다.
