# 날짜별 계획 경로 API

`TravelPlanRoute` 1차 구현. 기존 `/api/directions/driving`은 그대로 유지한다.

## 조회

```http
GET /api/plans/{planId}/routes
GET /api/plans/{planId}/routes?date=2026-09-10
Authorization: Bearer <accessToken>
```

기존 JWT 쿠키 인증도 지원한다. 본인 계획만 조회할 수 있다.
`date`는 선택적인 ISO 날짜이며 생략하면 저장된 전체 경로를 날짜 오름차순으로 반환한다.
일치하는 날짜가 없거나 아직 갱신하지 않은 기존 계획이면 `routes: []`다. 조회는 Directions를 호출하지 않는다.

| 필드 | 의미 |
|---|---|
| date | 경로 날짜 |
| status | READY / FAILED / UNSUPPORTED / NOT_REQUIRED / CALCULATING |
| option | traoptimal 고정 |
| distance | 총 meter, READY 외 null |
| duration | 총 millisecond, READY 외 null |
| calculatedAt | 성공 결과 저장 시각, ISO UTC, READY 외 null |
| path | `[longitude, latitude]` 배열 목록, READY 외 빈 배열 |
| failureCode | 실패 사유 또는 제한 초과 사유, 그 외 null |

`routeHash`는 응답에서 제외한다. null 필드의 출력 여부는 공통 Jackson 설정을 따른다.
계획 없음은 `404 PLAN404_1`, 다른 사용자의 계획은 `403 PLAN403_1`, 인증 없음은 401,
잘못된 date는 공통 오류 봉투와 함께 400을 반환한다.

READY 응답 형식(좌표/수치는 테스트 Directions 응답 fixture):

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공적으로 요청을 처리했습니다.",
  "result": {
    "routes": [{
      "date": "2026-09-10",
      "status": "READY",
      "option": "traoptimal",
      "distance": 18342,
      "duration": 2421000,
      "calculatedAt": "2026-09-07T00:00:00Z",
      "path": [[126.5, 33.5], [126.6, 33.6]],
      "failureCode": null
    }]
  }
}
```

## 생성·갱신

1. `POST /api/plans`: 계획 및 모든 날짜의 출발지·장소 저장 완료 후 이벤트 발행.
2. `PUT /api/plans/{planId}`: 전체 replace 완료 후 이벤트를 한 번 발행. 중간 삭제/삽입에는 호출 없음.
3. `POST /api/trips/{tripId}/waypoints`: 추가와 시작/도착 플래그 재정렬 완료 후 이벤트 발행.
4. `DELETE /api/trips/{tripId}/waypoints/{waypointId}`: 삭제 및 최종 순번 확정 후 이벤트 발행.
5. `PUT /api/trips/{tripId}/waypoints/order`: 날짜 전체 순서 및 플래그 확정 후 이벤트 발행.

```json
{"visitDate":"2026-09-10","waypointIds":[3,1,2]}
```

진행 중 순서 변경은 IN_PROGRESS에서만 가능하며 방문/건너뛰기 처리된 경유지 위치를 유지해야 한다.
기존 WaypointService의 DRAFT 재정렬도 같은 갱신 이벤트를 발행한다.
방문 인증·건너뛰기·선호 토글은 좌표 순서를 변경하지 않으므로 갱신 이벤트를 발행하지 않는다.

이벤트는 커밋 후 동기 실행한다. 외부 호출 동안 DB 트랜잭션이나 계획 잠금을 유지하지 않는다.
계획 기간의 각 날짜를 재판단하지만 동일 hash의 READY 날짜는 외부 호출을 생략한다.

입력은 저장된 DayDeparture 좌표 → sequenceOrder 순 Place 좌표다. DayDeparture가 없으면 첫 Place부터 시작한다.
전체 7지점까지 가능하고 8개 이상은 `UNSUPPORTED / TOO_MANY_POINTS`, 2개 미만은 `NOT_REQUIRED`다.
Directions 또는 Redis 호출 오류는 `FAILED`로 남기며 이미 커밋한 계획을 롤백하지 않는다.
정상적으로 반환된 도메인 오류는 기존 `DIRECTION...` 코드를 failureCode로 저장한다.

## 제한

저장 요청은 동기 경로 계산을 기다린다. 프로세스 중단이나 경로 결과 저장 DB 장애로 `CALCULATING`이
남을 수 있고 자동 재시도는 없다. 이후 계획 저장 또는 경유지 변경으로 재계산한다.
READY는 시간 경과만으로 갱신하지 않는다. 좌표를 직접 변경하는 Place 동기화는 이번 이벤트 범위 밖이다.
자세한 트랜잭션·동시성 설계는 [ADR-0012](adr/0012-travel-plan-route.md)를 참고한다.

## 실제 로컬 HTTP 검증 응답

2026-09-07, 별도 PostGIS DB에서 계획 30을 생성·수정한 뒤 조회한 실제 응답이다.
Directions 호출이 실패했지만 계획 수정은 HTTP 200으로 완료되고 해당 날짜만 FAILED로 남았다.
READY의 실제 네이버 성공 응답은 이번 실행에서 확보하지 못했고, READY 저장은 mock 기반 통합 테스트로 검증했다.

```json
{
  "isSuccess": true,
  "code": "COMMON200",
  "message": "성공적으로 요청을 처리했습니다.",
  "result": {
    "routes": [
      {
        "date": "2026-09-10",
        "status": "FAILED",
        "option": "traoptimal",
        "distance": null,
        "duration": null,
        "calculatedAt": null,
        "path": [],
        "failureCode": "DIRECTION502_1"
      }
    ]
  }
}
```
