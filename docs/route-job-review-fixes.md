# TravelPlanRoute CodeRabbit 리뷰 2건 수정

## 1. 실제 원인

- `prepare()`는 READY+동일 hash만 재사용하고 CALCULATING의 소유자/생존 여부를 알 수 없었다.
  따라서 중복 이벤트가 같은 입력을 pending에 넣어 Directions를 다시 호출했다.
- `PlanRouteChanged`와 AFTER_COMMIT 리스너는 메모리에만 존재했다.
  DB 커밋 직후 프로세스가 종료되면 계획은 저장돼도 경로 갱신 요청이 유실됐다.

## 2. 해결 구조

PostgreSQL의 `travel_plan_route_update_job`에 계획당 한 row를 저장한다.
계획 변경 트랜잭션의 MANDATORY enqueue, 요청 병합용 dirty, SKIP LOCKED claim,
UUID token/lease, 독립 heartbeat, backoff retry를 사용한다. 별도 MQ나 인프라는 없다.
worker는 기존 @Scheduled + ConditionalOnProperty scheduler 패턴으로 동작한다.

## 3. 신규/변경/삭제 파일

- 변경 `docs/adr/0012-travel-plan-route.md`
- 변경 `docs/adr/README.md`
- 변경 `docs/architecture.md`
- 변경 `docs/plan-routes.md`
- 삭제 `src/main/java/com/example/jejugilmoa/domain/plan/event/PlanRouteChanged.java`
- 변경 `src/main/java/com/example/jejugilmoa/domain/plan/service/TravelPlanRouteService.java`
- 변경 `src/main/java/com/example/jejugilmoa/domain/plan/service/TravelPlanRouteStore.java`
- 변경 `src/main/java/com/example/jejugilmoa/domain/plan/service/TravelPlanService.java`
- 변경 `src/main/java/com/example/jejugilmoa/domain/plan/service/WaypointService.java`
- 변경 `src/test/java/com/example/jejugilmoa/domain/plan/service/TravelPlanRouteIntegrationTest.java`
- 변경 `src/test/java/com/example/jejugilmoa/domain/plan/service/TravelPlanRouteServiceTest.java`
- 신규 `docs/adr/0013-durable-route-update-job.md`
- 신규 `docs/route-job-review-fixes.md`
- 신규 `src/main/java/com/example/jejugilmoa/domain/plan/dto/TravelPlanRouteJobClaim.java`
- 신규 `src/main/java/com/example/jejugilmoa/domain/plan/entity/TravelPlanRouteUpdateJob.java`
- 신규 `src/main/java/com/example/jejugilmoa/domain/plan/enums/TravelPlanRouteJobStatus.java`
- 신규 `src/main/java/com/example/jejugilmoa/domain/plan/repository/TravelPlanRouteJobRepository.java`
- 신규 `src/main/java/com/example/jejugilmoa/domain/plan/service/TravelPlanRouteJobService.java`
- 신규 `src/main/java/com/example/jejugilmoa/domain/plan/service/TravelPlanRouteWorker.java`
- 신규 `src/main/java/com/example/jejugilmoa/global/scheduler/TravelPlanRouteUpdateScheduler.java`
- 신규 `src/main/resources/db/migration/V37__travel_plan_route_update_job.sql`
- 신규 `src/test/resources/application.properties`

## 4. DB migration

신규 `V37__travel_plan_route_update_job.sql`만 추가했다. V1~V36은 수정하지 않았다.

| 항목 | 내용 |
|---|---|
| id | bigint identity PK |
| plan_id | bigint FK → travel_plan, UNIQUE, ON DELETE CASCADE |
| status | varchar(20), PENDING/RUNNING/DONE CHECK |
| dirty | boolean, 실행 중 새 요청 유입 표시 |
| attempt_count | integer, 연속 시도 횟수 |
| next_attempt_at | timestamptz, 재시도 가능 시각 |
| lease_until / lease_token | timestamptz / UUID, 현재 소유권 |
| last_error | varchar(100), 최근 작업 실패 분류 |
| created_at / updated_at | timestamptz |
| 추가 인덱스 | (status, next_attempt_at), (status, lease_until) |
| lease CHECK | RUNNING만 lease/token 필수, 나머지 상태는 둘 다 null |

## 5. 전체 처리 흐름

1. TravelPlanService.create/replace 또는 WaypointService 추가/삭제/재정렬이 최종 상태를 저장한다.
2. 같은 트랜잭션에서 job을 UPSERT한다. TripService 경유 호출도 최상위 트랜잭션에 참여한다.
3. 커밋 후 PENDING 작업이 DB에 남는다. rollback이면 계획 변경과 job 변경 모두 사라진다.
4. scheduler가 worker.runOnce를 호출한다. worker는 짧은 REQUIRES_NEW 트랜잭션으로 작업 한 개를 claim한다.
5. 짧은 입력 준비 트랜잭션에서 계획과 job 소유권을 확인하고 날짜별 최종 입력을 만든다.
6. 트랜잭션 밖에서 기존 DirectionService를 호출한다. 별도 heartbeat가 lease를 유지한다.
7. 결과 저장 트랜잭션에서 계획 → job 순서로 잠그고 token/lease 및 현재 입력 hash를 확인한다.
8. 모든 날짜가 성공하면 DONE, 새 요청 dirty가 있으면 PENDING으로 남긴다. 실패는 backoff 후 재시도한다.

기존 날짜별 경로 정책, hash, JSONB 좌표 순서, status, 조회 응답 형식은 유지했다.
계획 저장 응답은 이제 Directions 계산 완료를 기다리지 않는다.
새 계획의 첫 worker 처리 전 GET routes는 빈 목록일 수 있다.

## 6. 중복 실행 방지

- UNIQUE(plan_id)로 연속 요청을 병합한다. RUNNING 중 enqueue는 lease/token을 건드리지 않고 dirty만 표시한다.
- SELECT FOR UPDATE SKIP LOCKED + UPDATE RETURNING으로 한 worker만 유효한 token을 받는다.
- 활성 lease는 다른 worker가 claim할 수 없어 동일 hash CALCULATING이 중복 계산되지 않는다.
- 입력 준비/결과 저장은 유효한 claim을 요구한다. 무조건 CALCULATING skip 방식은 사용하지 않는다.
- READY+hash 재사용은 유지한다. 처리 중 같은 입력을 여러 번 저장해도 다음 job에서 재호출하지 않는다.
- 완료/lease 갱신도 token과 미만료 조건으로 제한해 이전 worker가 최신 job을 종료하거나 부활시키지 못한다.

## 7. 프로세스 중단 복구

- commit 직후 종료: PENDING이 DB에 남아 재시작 후 소비된다.
- claim/계산 중 종료: RUNNING lease 만료 후 새 token으로 재claim하고 CALCULATING을 다시 계산한다.
- route 저장 후 job 완료 전 종료: 재claim 시 READY+hash를 재사용하고 완료 처리한다.
- 오래된 입력/소유권의 늦은 결과는 차단한다. 입력 불일치로 버린 결과도 job 완료로 처리하지 않고 재시도한다.

외부 요청 자체는 장애 상황의 exactly-once를 보장하지 않는다. heartbeat가 끊긴 채 lease를 넘겨 정지했던
프로세스의 외부 요청이 아직 살아 있으면 재claim 요청과 겹칠 수 있다. 이전 소유권의 DB 반영은 차단한다.
V36 시절 이미 job 없이 남아 있던 CALCULATING을 스캔하는 backfill은 이번 범위에 포함하지 않았다.

## 8. retry / lease 정책

| 설정 | 정책 |
|---|---|
| poll | 기본 시작 지연 10초, fixed delay 1초, 한 번에 job 1개 |
| 활성화 | app.plan-route.worker.enabled, 기본 true |
| poll 조정 | app.plan-route.worker.poll-delay-ms / initial-delay-ms |
| lease | 120초, DB 시계 기준 |
| heartbeat | 독립 daemon executor에서 30초마다 갱신, 날짜별 호출 직전에도 갱신 |
| 재시도 | 30 → 60 → 120 → 240 → 480 → 900초, 이후 900초 유지 |
| 성공 | 연속 시도 횟수 초기화 |
| 실패 | FAILED route + PENDING job, 재시도 가능 |
| 영구 포기 | 없음. 최대 15분 간격으로 재시도 기회 유지 |

새 enqueue가 기존 PENDING backoff를 초기화하지 않는다. DB 장애로 finish 기록이 실패해도 lease 만료로 복구한다.
실패가 외부 호출에서 발생하므로 이미 커밋된 계획은 rollback되지 않는다.
반면 job 등록 자체가 실패하면 durable 보장을 위해 계획 트랜잭션도 실패한다.

## 9. PlanRouteChanged

클래스와 publish 호출, AFTER_COMMIT 리스너를 제거했다. 알림/힌트로도 남기지 않았다.
TravelPlanService/WaypointService는 `TravelPlanRouteJobService.enqueue()`를 호출한다.
TripService는 기존 WaypointService 위임 구조를 그대로 사용한다.

## 10. 테스트 및 결과

`DB_NAME=jejugilmoa_route_job_verify DB_PORT=5433 ./gradlew build`

**369 tests, failures 0, errors 0, skipped 0.** 기존 359개에 검증 시나리오 10개를 추가했다.
기존 경로 통합 테스트 8개는 worker를 명시 호출하도록 조정하고 검증 의미를 유지했다.

| 요구 시나리오 | 검증 |
|---|---|
| 계획/job 동일 트랜잭션 | 트랜잭션 내부 PENDING 확인, 독립 트랜잭션에서는 미커밋 변경 미노출 |
| rollback | 생성 rollback 시 job 없음, 전체 수정 rollback 시 기존 DONE 복원 |
| commit 후 이벤트와 무관한 잔존 | worker 미실행 상태에서 DB PENDING/경로 없음 확인 |
| worker 소비 | PENDING → RUNNING → DONE, route READY |
| 두 worker 동시 claim | 별도 스레드 동시 claim 결과 소유자 정확히 1명 |
| CALCULATING 중복 방지 | Directions 호출을 latch로 유지한 상태에서 반복 저장/두 번째 worker 실행, 총 호출 1회 |
| stale lease | 만료 강제 후 재claim, CALCULATING → READY |
| heartbeat | 현재 token의 갱신으로 lease 연장, 다른 claim 거부, 만료 token 갱신 거부 |
| READY 재사용 | 메타 변경 후 hash/계산 시각 유지, 추가 외부 호출 없음 |
| 새 입력 | replace 순서/출발지/진행 중 waypoint 변경 후 재계산 |
| 외부 실패 재시도 | FAILED/PENDING, 즉시 claim 거부, 재시도 시각 이후 새 계획 변경 없이 READY |
| 늦은 결과 차단 | 현재 입력 불일치 및 같은 hash라도 만료 소유권의 결과/finish 거부 |
| 버린 결과의 복구 | 계산 도중 Place 좌표 변경 시 결과 폐기 후 job 재시도로 정상 완료 |
| 기존 API/정책 | 생성/수정/추가/삭제/순서 변경, JSONB, 제한 초과, 인증/권한 등 기존 테스트 유지 |

실제 앱/HTTP 검증:

- 별도 PostGIS DB에서 V37 적용과 테이블/인덱스/제약 확인.
- worker 비활성화 앱에서 계획 34 생성: HTTP 201, routes 빈 목록, DB job PENDING/attempt 0.
- 앱 종료 후 worker 활성화로 재시작: 별도 HTTP 변경 없이 scheduler가 기존 job 처리.
- GET routes 200/NOT_REQUIRED, DB job DONE/dirty=false/lease와 token null 확인.
- 기존 미인증 401, 없는 계획 404, Swagger의 routes 및 /api/directions/driving 유지 확인.
- 검증용 앱은 종료했다. 정상 Directions 및 장애/동시성은 mock 외부 응답과 실제 PostGIS로 검증했다.
- `git diff --check` 통과, 기존 migration 변경 없음 확인.

## 11. 범위 외 추가 여부

새 사용자 기능은 추가하지 않았다. API 스펙, TravelRecordRoute, chunk/segment, backfill은 변경/추가하지 않았다.
입력 불일치로 결과를 버린 경우 재시도하는 보완은 CALCULATING이 남은 채 job이 DONE이 되는 것을 막기 위한
동일 리뷰 범위의 수정이다. ERD/ADR/API 설명은 실행 방식 변경에 맞춰 갱신했다.
기존 migration 수정, stash/shelve, 커밋은 수행하지 않았다.
