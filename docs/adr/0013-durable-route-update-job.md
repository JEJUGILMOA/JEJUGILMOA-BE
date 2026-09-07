# ADR-0013: 경로 갱신 durable job과 lease 복구

- 상태: Accepted
- 날짜: 2026-09-07
- 대체: ADR-0012의 메모리 이벤트, 동기 계산, 자동 복구 부재

## 원인

READY만 재사용하는 prepare는 활성 CALCULATING과 중단된 CALCULATING을 구분할 수 없다.
AFTER_COMMIT 메모리 이벤트는 DB 커밋 직후 프로세스가 종료되면 갱신 요청을 잃는다.

## 결정

V37에서 `travel_plan_route_update_job`을 추가한다. 계획당 유니크 row 하나로 요청을 합친다.
계획/경유지 저장 트랜잭션에서 MANDATORY enqueue를 호출한다. job 저장 실패도 계획과 함께 롤백한다.
TripService가 WaypointService를 호출할 때도 최상위 트랜잭션을 그대로 사용한다.
PlanRouteChanged와 AFTER_COMMIT 리스너를 제거한다.

상태는 PENDING, RUNNING, DONE이다. enqueue는 PENDING의 예약 시각/backoff를 유지하며,
RUNNING의 token/lease를 바꾸지 않고 dirty=true만 표시한다. DONE이면 PENDING으로 다시 연다.
완료 시 dirty가 있으면 PENDING으로 남겨 계산 중 들어온 변경을 유실하지 않는다.
현재 DB의 최종 좌표를 읽으므로 연속 변경은 한 요청으로 합쳐지고, READY+hash가 같으면 외부 호출을 생략한다.

worker는 기존 @Scheduled/ConditionalOnProperty 패턴을 사용한다. 기본 시작 지연 10초, fixed delay 1초,
poll당 최대 한 job을 순차 처리한다. `app.plan-route.worker.enabled=false`로 poll을 끌 수 있다.
`poll-delay-ms`, `initial-delay-ms`를 설정할 수 있다.

짧은 REQUIRES_NEW 트랜잭션에서 `FOR UPDATE SKIP LOCKED`와 UPDATE RETURNING으로 원자적 claim한다.
DB 시계로 만료를 판단하고 매 claim마다 새 UUID 소유권 토큰을 부여한다. lease는 120초이다.
별도 daemon executor의 heartbeat가 30초마다 현재 소유권과 미만료 조건으로 lease를 갱신한다.
날짜별 외부 호출 직전에도 갱신한다. 기존 단일 scheduler의 외부 호출 대기가 heartbeat를 막지 않는다.
worker 종료 시 executor를 정리한다.

입력 준비와 결과 저장만 짧은 트랜잭션이며 외부 호출에는 트랜잭션을 유지하지 않는다.
prepare/complete는 계획 → job 순서로 잠근다(enqueue와 동일 순서). 유효한 token/lease가 필수다.
활성 job은 다른 worker가 claim할 수 없어 동일 hash CALCULATING을 중복 준비하지 않는다.
만료 후 새 token으로 회수한 worker는 CALCULATING도 다시 준비한다. CALCULATING을 무조건 skip하지 않는다.
결과는 기존 현재 입력 hash/route hash 검증에 더해 job token으로 fencing한다.

모든 날짜 처리 성공이면 DONE, 처리 도중 새 변경이 있었으면 PENDING이다.
Directions FAILED, 준비/결과 저장 예외, 현재 입력 hash 불일치로 버린 결과는 PENDING으로 남겨 재시도한다.
재시도는 30, 60, 120, 240, 480, 900초이며 이후 900초 상한을 유지한다. 성공하면 횟수를 초기화한다.
무한 즉시 retry는 없고, 실패 횟수에 따른 영구 포기도 없어 복구 가능한 장애는 계속 회복 기회를 갖는다.
DB 자체가 접근 불가하면 완료/재시도 기록도 실패할 수 있지만 기존 RUNNING row는 lease 만료로 회수된다.

## 경계와 한계

- 새 API, MQ, TravelRecordRoute, chunk/segment, backfill은 추가하지 않는다. 기존 migration은 변경하지 않는다.
- GET routes 형식은 유지한다. 계획 저장은 이제 경로 계산을 기다리지 않는다.
- commit 이전 종료는 계획/job 모두 롤백, commit 이후 claim 이전 종료는 PENDING이 남는다.
  claim 이후 종료는 RUNNING lease 만료 후 재처리한다. 일부 날짜가 READY이면 재사용한다.
- 정상적으로 heartbeat하는 활성 작업은 중복 소유하지 않는다. 만료된 소유권은 부활시킬 수 없다.
- lease를 넘는 전체 프로세스 정지/네트워크 단절 뒤 이전 외부 요청이 여전히 실행 중일 경우,
  회수한 worker의 외부 요청과 겹칠 가능성은 있다. 외부 Directions에는 트랜잭션/idempotency 연계가 없으므로
  외부 호출의 장애 상황 exactly-once를 보장하지 않는다. 이전 worker의 DB 결과/완료 반영은 token으로 차단한다.
- V36 시절 이미 유실된 이벤트나 job 없는 기존 CALCULATING을 스캔해서 생성하지 않는다.
  이번 변경 이후 등록된 job만 복구 대상이다(기존 계획 backfill 제외 요구사항).
