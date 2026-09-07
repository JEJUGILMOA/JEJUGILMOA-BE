# TravelPlanRoute 1차 구현 결과

## 1. 실제 변경 파일

- `docs/adr/README.md`
- `docs/architecture.md`
- `src/main/java/com/example/jejugilmoa/domain/direction/service/DirectionService.java`
- `src/main/java/com/example/jejugilmoa/domain/plan/controller/TripController.java`
- `src/main/java/com/example/jejugilmoa/domain/plan/service/TravelPlanService.java`
- `src/main/java/com/example/jejugilmoa/domain/plan/service/TripService.java`
- `src/main/java/com/example/jejugilmoa/domain/plan/service/WaypointService.java`
- `docs/adr/0012-travel-plan-route.md`
- `docs/plan-routes-implementation.md`
- `docs/plan-routes.md`
- `src/main/java/com/example/jejugilmoa/domain/plan/controller/TravelPlanRouteController.java`
- `src/main/java/com/example/jejugilmoa/domain/plan/converter/TravelPlanRouteConverter.java`
- `src/main/java/com/example/jejugilmoa/domain/plan/dto/TravelPlanRoutesResponse.java`
- `src/main/java/com/example/jejugilmoa/domain/plan/entity/TravelPlanRoute.java`
- `src/main/java/com/example/jejugilmoa/domain/plan/enums/TravelPlanRouteStatus.java`
- `src/main/java/com/example/jejugilmoa/domain/plan/event/PlanRouteChanged.java`
- `src/main/java/com/example/jejugilmoa/domain/plan/repository/TravelPlanRouteRepository.java`
- `src/main/java/com/example/jejugilmoa/domain/plan/service/PlanRouteInput.java`
- `src/main/java/com/example/jejugilmoa/domain/plan/service/TravelPlanRouteService.java`
- `src/main/java/com/example/jejugilmoa/domain/plan/service/TravelPlanRouteStore.java`
- `src/main/resources/db/migration/V36__travel_plan_route.sql`
- `src/test/java/com/example/jejugilmoa/domain/plan/controller/TravelPlanRouteControllerTest.java`
- `src/test/java/com/example/jejugilmoa/domain/plan/controller/TripWaypointOrderControllerTest.java`
- `src/test/java/com/example/jejugilmoa/domain/plan/service/TravelPlanRouteIntegrationTest.java`
- `src/test/java/com/example/jejugilmoa/domain/plan/service/TravelPlanRouteServiceTest.java`

## 2. 추가된 DB 스키마

신규 `V36__travel_plan_route.sql`만 추가했다. 기존 Flyway 파일은 수정하지 않았다.

| 컬럼 | PostgreSQL 타입 | 조건 |
|---|---|---|
| id | bigint identity | PK |
| plan_id | bigint | NOT NULL, travel_plan FK, ON DELETE CASCADE |
| route_date | date | NOT NULL |
| path | jsonb | nullable, 배열 타입 CHECK |
| total_distance | integer | nullable, meter |
| total_duration | bigint | nullable, millisecond |
| route_option | varchar(20) | NOT NULL, 구현상 traoptimal 고정 |
| route_hash | varchar(64) | NOT NULL, SHA-256 hex |
| status | varchar(20) | NOT NULL, 5개 상태 CHECK |
| failure_code | varchar(100) | nullable |
| calculated_at | timestamptz | nullable, READY 성공 시각 |
| created_at / updated_at | timestamptz | NOT NULL, BaseEntity auditing |

`uk_plan_route_date UNIQUE(plan_id, route_date)` 적용. 상태는 READY, FAILED, UNSUPPORTED,
NOT_REQUIRED, CALCULATING이다. JSONB는 `[longitude, latitude]` 배열 목록으로 저장한다.
기존 TravelCourse 및 그 스키마는 유지했다.

## 3. 생성·갱신 흐름

계획 저장 트랜잭션 → 최종 상태에서 PlanRouteChanged 발행 → 커밋 → 동기 AFTER_COMMIT 리스너 →
REQUIRES_NEW 입력 준비/계획 잠금 → 잠금 해제 → 트랜잭션 없이 DirectionService 호출 →
REQUIRES_NEW 결과 저장/계획 잠금/현재 hash 재검증 순서다.

입력은 날짜별 DayDeparture 좌표 다음 TravelCourse.sequenceOrder 순 Place 좌표다.
DayDeparture 없는 기존 계획은 첫 Place부터 시작한다. 계획 기간의 비어 있는 날짜도 NOT_REQUIRED로 저장한다.
기존 READY와 hash가 같으면 입력 준비 단계에서 건너뛴다. 재계산 대상은 CALCULATING으로 바꾸며
기존 path/거리/시간/calculatedAt을 비운다. 늦게 도착한 이전 입력 결과는 버린다.

## 4. routeHash 기준

DB에서 읽은 좌표를 실제 DirectionService에 전달하는 double로 정규화한다.
소수 표기 자릿수 차이와 음수 0을 통일한 `traoptimal\n경도,위도|경도,위도...`의 UTF-8 SHA-256이다.
순서 및 중복 좌표는 보존한다. 날짜, 장소 ID, 제목, 예산, 메모, 선호 여부는 포함하지 않는다.

## 5. 각 API의 갱신 시점

| API | 갱신 시점 |
|---|---|
| POST /api/plans | 모든 출발지/장소 저장 후 이벤트 1회, 커밋 후 계산 |
| PUT /api/plans/{planId} | 전체 replace 최종 상태에서 이벤트 1회, 커밋 후 계산 |
| POST /api/trips/{tripId}/waypoints | 추가 및 최종 순번/플래그 확정 후, 최상위 Trip 트랜잭션 커밋 후 계산 |
| DELETE /api/trips/{tripId}/waypoints/{waypointId} | 삭제 및 최종 순번/플래그 확정 후, 커밋 후 계산 |
| PUT /api/trips/{tripId}/waypoints/order | 날짜 전체 순서 확정 후, 커밋 후 계산 |

공개 순서 변경 API를 새로 연결했다. 진행 중에만 허용하며 방문/건너뛴 경유지는 원래 위치를 유지한다.
기존 WaypointService의 DRAFT 순서 변경도 동일 이벤트를 발행한다.
방문 인증, 건너뛰기, 선호 변경은 좌표 순서를 바꾸지 않아 계산을 호출하지 않는다.

## 6. 외부 Directions 실패

GeneralException은 기존 DIRECTION 오류 코드를 failureCode로 저장한다. 기타 호출/캐시 예외도 FAILED로 저장한다.
계획 생성/수정은 이미 커밋되어 롤백되지 않고 정상 API 응답을 반환한다. 같은 입력의 FAILED도 다음 저장 때 재시도한다.
실제 HTTP에서 계획 수정 200 이후 routes 조회 200, `FAILED / DIRECTION502_1`을 확인했다.

## 7. waypoint 제한 초과

Directions 5의 최대 경유지 5개 + 출발/도착을 합한 전체 7지점까지 계산한다.
8지점 이상이면 외부 호출 없이 `UNSUPPORTED / TOO_MANY_POINTS`, 2지점 미만이면 `NOT_REQUIRED`다.
두 경우 계획 저장은 성공한다. 제한 상수는 기존 DirectionService와 공유한다.
[공식 명세](https://api.ncloud-docs.com/docs/application-maps-directions5)와 대조했다.

## 8. GET routes API 및 실제 응답

`GET /api/plans/{planId}/routes?date=YYYY-MM-DD` — date 선택, 본인만 조회, 날짜 오름차순,
ApiResponse 봉투 사용. routeHash 비공개. 매칭 없음/아직 계산하지 않은 기존 계획은 빈 routes다.
미인증 401, 타인 계획 403, 계획 없음 404, 잘못된 date 400.
필드 명세·READY fixture 예시·실제 FAILED HTTP 응답은 [API 문서](plan-routes.md)에 기록했다.

## 9. 실행한 테스트와 결과

- Docker Compose PostGIS + Redis 기동, 별도 `jejugilmoa_route_verify` DB 생성.
- `DB_NAME=jejugilmoa_route_verify DB_PORT=5433 ./gradlew build`: 최종 **359 tests, 0 failures/errors/skips**.
- 신규 테스트 18개: 서비스 단위 5, PostGIS 통합 8, routes MVC 3, 공개 순서 변경 MVC 2.
- 통합 테스트: 커밋 후 호출, 외부 호출 중 트랜잭션 없음 및 별도 트랜잭션의 계획 잠금 획득,
  READY 재사용, 최종 replace 순서, 출발지 변경, 실패/재시도, 롤백 시 무호출, 7/8지점 경계,
  기존 출발지 fallback, 오래된 결과 차단, 진행 중 추가/삭제/순서 변경, 방문 위치 보호,
  JSONB 배열 저장, 본인 확인, 날짜 필터, 계획 삭제 cascade.
- `DB_NAME=jejugilmoa_route_verify DB_PORT=5433 SERVER_PORT=18080 APP_SYNC_RUN_ON_STARTUP=false ./gradlew bootRun`.
- 실제 HTTP 13개 요청 검증: 계획 생성/수정, routes 전체/날짜/빈 결과, NOT_REQUIRED, UNSUPPORTED,
  Directions 실패 후 FAILED 및 계획 보존, 400/401/403/404 응답.
- `/v3/api-docs`: routes, 공개 순서 변경, 기존 directions 경로 존재 확인.
- `psql \d travel_plan_route`: JSONB, UNIQUE, FK cascade, 상태 CHECK 확인.
- `git diff --check`: 통과.
- READY 경로는 mock Directions 응답을 사용해 서비스 및 DB 저장을 검증했다.
  실제 네이버 성공 경로 호출은 이번 실행에서 확보하지 못했다.

## 10. 발견한 위험 및 후속 개선 사항

- 기존 삭제의 일괄 sequenceOrder - 1 UPDATE가 PostgreSQL 행 처리 순서에 따라 UNIQUE 충돌을 냈다.
  통합 테스트에서 재현했고 임시 순번 후 최종 순번 부여로 수정했다. 기존 스키마 변경은 없다.
- 기존 공개 API에 진행 중 순서 변경이 없어서 이번에 추가했다.
- 커밋 직후 이벤트 실행 전에 프로세스가 종료되면 경로 갱신을 놓칠 수 있다.
  계산 도중 중단/결과 저장 DB 장애 시 CALCULATING이 남을 수 있다. 자동 재시도는 없고 다음 저장 때 재판단한다.
- 계산은 동기식이어서 저장 응답 시간이 날짜별 외부 호출만큼 늘어난다.
- 동일 입력의 동시 미완료 계산은 중복 외부 호출 가능성이 있다. 오래된 좌표의 결과 저장은 차단한다.
- Directions의 기존 Redis 캐시 장애도 FAILED로 처리된다. READY는 시간 만료 없이 재사용한다.
- Place 좌표 동기화는 즉시 경로를 갱신하지 않는다. 다음 계획/경유지 변경 때 hash로 반영한다.
- 기존 계획 backfill, TravelRecordRoute, scheduler/polling worker, revision/input_snapshot,
  RouteSegment, chunk는 구현하지 않았다. stash/shelve 및 커밋은 수행하지 않았다.
