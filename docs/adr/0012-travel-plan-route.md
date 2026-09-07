# ADR-0012: 날짜별 계획 경로 저장과 커밋 후 계산

- 상태: Accepted
- 날짜: 2026-09-07

## 배경

계획에 저장된 장소 순서대로 지도 폴리라인을 재사용해야 한다. 외부 Directions 장애가 계획 저장을
롤백시키면 안 되고, 진행 중 경유지 변경에 사용하는 계획 행 잠금을 외부 응답 동안 유지할 수 없다.

## 결정

- `TravelCourse`를 유지하고 V36에서 `travel_plan_route`를 추가한다. 날짜별 `UNIQUE(plan_id, route_date)`,
  계획 삭제 시 FK의 `ON DELETE CASCADE`를 사용한다. 신규 엔티티는 부모 컬렉션 없이 단방향으로 참조한다.
- 날짜별 입력은 저장된 `DayDeparture` 좌표 다음 `sequenceOrder` 오름차순 장소 좌표다.
  출발지가 없는 기존 계획은 첫 장소부터 시작한다. 계획 기간의 빈 날짜도 `NOT_REQUIRED`로 저장한다.
- hash는 Directions에 전달할 double 좌표를 정규화한 `traoptimal\n경도,위도|경도,위도...`의 UTF-8 SHA-256이다.
  소수 자릿수 표기 차이와 음수 0은 정규화한다. 좌표 중복은 제거하지 않는다. 날짜·제목·예산·장소 ID는 제외한다.
- 같은 hash의 `READY`는 재사용한다. `FAILED`/`CALCULATING`은 다음 계획 저장 또는 경유지 변경 때 다시 계산한다.
- `AFTER_COMMIT` 동기 이벤트 리스너에 `NOT_SUPPORTED`를 적용한다. `REQUIRES_NEW` 입력 준비 트랜잭션에서
  계획을 잠그고 `CALCULATING`으로 바꾸며 이전 path/거리/시간을 지운다. 외부 호출은 트랜잭션 없이 수행한다.
  `REQUIRES_NEW` 결과 반영 트랜잭션에서 계획을 잠그고 현재 입력 hash와 저장 route hash를 모두 확인한다.
- 현재 입력과 다른 늦은 결과는 버린다. 동일 입력에서 성공한 결과를 늦은 실패가 덮어쓰지 않는다.
- 기존 `DirectionService`를 재사용한다. DTO의 `{lat,lng}`를 JSONB `[lng,lat]` 배열로 명시 변환한다.
  거리 meter, 시간 millisecond를 변환 없이 저장한다.
- Directions 5의 경유지 5개 제한에 따라 총 7지점까지 계산한다.
  8지점 이상 `UNSUPPORTED`, 2지점 미만 `NOT_REQUIRED`, 호출 실패 `FAILED`로 저장한다.
- 진행 중 순서 변경을 `PUT /api/trips/{tripId}/waypoints/order`로 공개한다. 날짜 전체 ID를 받아야 하고
  이미 방문(건너뛰기 포함)한 경유지의 위치를 변경할 수 없다.

## 결과와 한계

- 외부 실패는 이미 커밋된 계획에 영향을 주지 않는다. 결과 저장 자체의 DB 장애는 로그를 남기며
  `CALCULATING`이 남을 수 있다. 프로세스 중단도 같은 제한이 있다.
- 커밋부터 리스너 입력 준비 사이의 짧은 구간에는 이전 경로가 조회될 수 있다. 이벤트는 내구성 큐가 아니므로
  커밋 직후 프로세스가 종료되면 갱신을 놓칠 수 있다. 자동 복구/재시도는 이번 범위에서 제외한다.
- HTTP 저장 응답은 동기 계산 완료까지 기다린다. 최대 날짜 수만큼 외부 응답 시간이 누적될 수 있다.
- 동시 동일 입력 요청이 아직 READY가 아닌 경우 외부 호출이 중복될 수 있지만 오래된 입력 결과는 반영하지 않는다.
- 기존 Directions Redis 캐시(30분)는 그대로 적용된다. Redis 장애도 FAILED가 될 수 있다.
- READY의 시간 기반 만료는 없다. 교통 상황 변화는 동일 hash 재계산을 유발하지 않는다.
- Place 좌표 데이터 동기화 자체에는 갱신 이벤트를 연결하지 않는다. 다음 계획/경유지 저장에서 재판단한다.
- 기존 계획 자동 backfill, TravelRecordRoute, scheduler/polling worker, revision/input_snapshot,
  RouteSegment, chunk는 구현하지 않는다.

## 참고

[네이버 Directions 5 공식 명세](https://api.ncloud-docs.com/docs/application-maps-directions5)
