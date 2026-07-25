# 0009. 경유지 동시 추가의 중복·순번 충돌 방지

- 상태: Accepted
- 날짜: 2026-07-26

## 배경 (Context)

`WaypointService.addWaypoint`는 경유지 추가 시 두 가지 검사를 수행한다.

1. `existsByTravelPlanIdAndPlaceId` — 같은 장소가 이미 코스에 있는지 확인
2. `countByTravelPlanId + 1` — 다음 `sequenceOrder` 계산

두 검사 모두 TOCTOU(time-of-check to time-of-use) 패턴이다. 동일한 계획에 거의 동시에 두 요청이 들어오면:

- **중복 장소**: pre-check을 두 요청 모두 통과해 같은 `(plan_id, place_id)` 행이 두 개 삽입됨
- **순번 충돌**: 두 요청이 같은 `nextOrder`를 계산해 `uk_course_plan_sequence` 제약 위반 발생 → `DataIntegrityViolationException`이 글로벌 핸들러까지 올라가 의도하지 않은 응답 반환

## 결정 (Decision)

두 문제를 서로 다른 계층에서 각각 막는다.

### 1. DB 유니크 제약으로 중복 장소 차단

`travel_course` 테이블에 `uk_course_plan_place(plan_id, place_id)` 유니크 제약을 추가한다.

```java
// TravelCourse.java
@UniqueConstraint(name = "uk_course_plan_place", columnNames = {"plan_id", "place_id"})
```

`addWaypoint`에서 `save` → `saveAndFlush`로 변경해 동일 트랜잭션 내에서 즉시 flush, 제약 위반을 `DataIntegrityViolationException`으로 받아 `PLACE_ALREADY_ADDED(PLAN400_6)`로 변환한다.

```java
try {
    travelCourseRepository.saveAndFlush(course);
} catch (DataIntegrityViolationException e) {
    throw new GeneralException(PlanErrorCode.PLACE_ALREADY_ADDED);
}
```

글로벌 핸들러(`GeneralExceptionAdvice`)에 도메인 코드를 임포트하면 레이어가 오염되므로, 도메인 특화 매핑은 서비스 계층에서 처리한다.

### 2. 비관적 락으로 순번 충돌 직렬화

`TravelPlanRepository`에 `SELECT FOR UPDATE` 쿼리를 추가한다.

```java
// TravelPlanRepository.java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM TravelPlan p WHERE p.id = :planId")
Optional<TravelPlan> findByIdForUpdate(@Param("planId") Long planId);
```

`WaypointService.findPlanAndVerifyOwner`에서 이 쿼리를 사용한다. `addWaypoint`와 `removeWaypoint` 모두 순번을 조작하므로 두 메서드 모두 적용된다.

같은 `plan_id`에 대한 락은 PostgreSQL 행 수준(row-level)으로 획득되므로, 서로 다른 계획 간에는 경합이 없다.

## 결과 (Consequences)

**좋아지는 것**
- 같은 장소 중복 추가가 DB 레벨에서 완전히 차단됨
- 동시 추가 시 순번이 항상 연속 정수로 보장됨
- race condition 상황에서도 `PLACE_ALREADY_ADDED` 도메인 에러를 정확히 반환함

**감수하는 것**
- 경유지 추가/삭제마다 plan 행에 비관적 락을 획득 — 같은 계획에 대한 쓰기 요청은 직렬화됨
- 정상 사용 패턴(한 사용자가 자신의 계획을 편집)에서는 다른 요청과 경합할 일이 없으므로 실질적인 성능 영향은 없음

**지켜야 하는 규칙**
- `sequenceOrder`를 변경하는 모든 로직(추가, 삭제, 순서 변경)은 반드시 `findByIdForUpdate`를 통해 plan을 조회해야 한다
- `findById`(락 없음)는 읽기 전용 조회(`listWaypoints` 등)에만 사용한다

### 3. `removeWaypoint` 순번 재정렬을 벌크 UPDATE로 교체

기존 구현은 오름차순으로 로딩한 엔티티들을 `decrementOrder()`로 수정한 뒤 Hibernate dirty-flush에 맡겼다.
Hibernate는 dirty 엔티티의 flush 순서를 보장하지 않으므로, 내부 최적화에 따라 `(plan_id, sequence_order)` 유니크 제약(`uk_course_plan_sequence`)을 임시로 위반하는 순서로 UPDATE가 발행될 수 있다.

이를 단일 벌크 JPQL로 교체한다.

```java
// TravelCourseRepository.java
@Modifying(flushAutomatically = true, clearAutomatically = true)
@Query("UPDATE TravelCourse c SET c.sequenceOrder = c.sequenceOrder - 1 WHERE c.travelPlan.id = :planId AND c.sequenceOrder > :removedOrder")
void decrementSequenceOrderAfter(@Param("planId") Long planId, @Param("removedOrder") int removedOrder);
```

- `flushAutomatically = true`: 쿼리 실행 전 펜딩된 DELETE가 먼저 DB에 반영되도록 보장
- `clearAutomatically = true`: 벌크 UPDATE 후 1차 캐시를 무효화해 이후 조회가 갱신된 값을 읽도록 보장

단일 SQL UPDATE는 삭제 후 비어 있는 순번부터 시작해 1씩 감소시키므로, 삭제된 행의 순번이 먼저 freed된 상태에서 업데이트가 진행된다.

## 고려한 대안 (Alternatives)

- **애플리케이션 레벨 락(synchronized, Redisson)** — 분산 환경에서 `synchronized`는 무의미하고, Redis 분산 락은 락 획득 실패 처리와 타임아웃 설계가 추가로 필요해 복잡도 대비 이득이 작음. DB 행 락으로 충분한 규모
- **글로벌 핸들러에서 constraint 이름 파싱** — `message.contains("uk_course_plan_place")`로 `PLACE_ALREADY_ADDED`를 반환하는 방법. 구현은 단순하나 글로벌 인프라 계층이 도메인 에러 코드에 의존하게 되어 레이어 규칙 위반
- **DB 시퀀스(NEXTVAL) 사용** — `sequenceOrder`를 DB 시퀀스로 할당하면 순번 충돌이 원천 차단됨. 단, 경유지 제거 후 순번 재정렬 로직과 충돌하므로 현재 설계와 맞지 않음
