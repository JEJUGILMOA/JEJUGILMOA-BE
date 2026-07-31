---
name: new-feature
description: Scaffold a complete API vertical slice (repository, service, controller, DTOs, converter, error codes) for a domain, following this repo's layering and response-envelope conventions. Use when implementing a new API endpoint or a new domain feature.
---

# Scaffold an API vertical slice

Authority on layering rules: `docs/architecture.md`. This skill is the executable recipe.
Target package layout inside `domain/<name>/`: `controller`, `service`, `repository`,
`dto`, `converter`, `entity`, `enums`, `exception`.

Work bottom-up so each layer compiles against the one below it.

## 1. Repository — `domain/<x>/repository/`

```java
public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {
    List<TravelPlan> findAllByUserIdAndStatus(Long userId, TravelPlanStatus status);
}
```

- Spatial queries must be native (see `docs/adr/0002-postgis-dual-storage.md`). `ST_MakePoint`
  always takes longitude first, latitude second:
  ```java
  @Query(value = """
      SELECT * FROM place p
      WHERE p.is_published = true
        AND ST_DWithin(p.geom, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
      """, nativeQuery = true)
  List<Place> findWithinRadius(double lat, double lng, double radiusMeters);
  ```
  Other spatial patterns already in the codebase, reuse rather than reinvent:
  - **Bounding-box / viewport queries** (`p.geom && ST_MakeEnvelope(:minLng,:minLat,:maxLng,:maxLat,4326)`)
    — see `PlaceRepository.findWithinBounds`.
  - **Grid-cell aggregation** (bucket lat/lng into an N×N grid with
    `width_bucket(col::float8, :min, :max, :gridSize)`, clamped with
    `LEAST(GREATEST(...,1),:gridSize)` to handle the exact-max boundary case) — see
    `TravelRecordPlaceRepository.aggregateVisitsByGrid` / `PopularPlaceRepository.aggregatePopularityByGrid`
    and how `MapQueryService` merges two such aggregates in Java rather than one SQL query
    (avoids join fan-out when the two source tables have different cardinality per place).
- Soft-deleted entities (`User`, `TravelRecord`): every query must filter `deletedAt IS NULL`
  (e.g. `findByIdAndDeletedAtIsNull`). This includes native queries that join through a
  soft-deleted entity, not just direct queries on it — e.g. a query joining
  `travel_record_place` → `travel_record` must still filter `tr.deleted_at IS NULL`.
  See `docs/adr/0003-soft-delete.md`.

## 2. Error codes — `domain/<x>/exception/`

New enum implementing `BaseCode`, code format `<DOMAIN><HTTP_STATUS>_<N>`, Korean messages.
Model: `global/apiPayload/code/GeneralErrorCode.java`. Never overload `COMMON...` codes
with domain meaning.

```java
@Getter
@AllArgsConstructor
public enum PlanErrorCode implements BaseCode {
    PLAN_NOT_FOUND(HttpStatus.NOT_FOUND, "PLAN404_1", "여행 계획을 찾을 수 없습니다."),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "PLAN400_1", "종료일이 시작일보다 빠를 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
```

## 3. DTOs — `domain/<x>/dto/`

Java records. Validation annotations on request DTOs (messages in Korean — they surface
verbatim in the error envelope via `GeneralExceptionAdvice`). Entities never cross the
controller boundary.

```java
public record TravelPlanCreateRequest(
        @NotBlank(message = "제목은 필수입니다.") @Size(max = 200) String title,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull TransportMode transportMode
) {}

public record TravelPlanResponse(Long id, String title, LocalDate startDate,
                                 LocalDate endDate, TravelPlanStatus status) {}
```

## 4. Converter — `domain/<x>/converter/`

`@Component` with instance methods (not static) — this is a Spring bean injected into the
service, matching `PlaceConverter`/`MapConverter`. Keeps mapping out of services; pure
mapping only, no thresholds/business logic here.

```java
@Component
public class TravelPlanConverter {
    public TravelPlan toEntity(TravelPlanCreateRequest req, User user) { ... }
    public TravelPlanResponse toResponse(TravelPlan plan) { ... }
}
```

## 5. Service — `domain/<x>/service/`

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelPlanService {
    private final TravelPlanRepository travelPlanRepository;
    private final TravelPlanConverter travelPlanConverter;

    @Transactional  // write methods override readOnly
    public TravelPlanResponse create(TravelPlanCreateRequest request, Long userId) {
        if (request.endDate().isBefore(request.startDate())) {
            throw new GeneralException(PlanErrorCode.INVALID_DATE_RANGE);
        }
        ...
    }

    public TravelPlanResponse getById(Long planId) {
        TravelPlan plan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new GeneralException(PlanErrorCode.PLAN_NOT_FOUND));
        return travelPlanConverter.toResponse(plan);
    }
}
```

Business failures always via `throw new GeneralException(<code>)` — never try-catch in
controllers, never return null/Optional to signal failure across the service boundary.

## 6. Controller — `domain/<x>/controller/`

```java
@Tag(name = "여행 계획", description = "여행 계획 API")
@RestController
@RequestMapping("/api/plans")
@RequiredArgsConstructor
public class TravelPlanController implements TravelPlanControllerDocs {
    private final TravelPlanService travelPlanService;

    @PostMapping
    public ApiResponse<TravelPlanResponse> create(
            @Valid @RequestBody TravelPlanCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal) {
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED,
                travelPlanService.create(request, principal.getUserId()));
    }
}
```

- Route prefix: `/api/<plural-resource>` — no `/v1` (not used anywhere in this codebase).
- Validate simple query params (page/size/limit/bounds) directly in the controller and
  `throw new GeneralException(<code>)` on failure; never try-catch. See `PlaceController`
  or `MapController` for the pattern.
- Put Swagger `@Operation`/`@ApiResponses`/example-JSON annotations in a sibling
  `controller/docs/<X>ControllerDocs` interface that the controller `implements`, not inline
  on the controller — keeps the controller readable. See `PlaceControllerDocs`/`MapControllerDocs`.
- **Auth is implemented** (JWT via `JwtAuthenticationFilter`, see `docs/auth.md` /
  ADR-0006) — `SecurityConfig` requires a valid JWT for every route by default. Get the
  current user via `@AuthenticationPrincipal UserPrincipal principal`, never a `TODO`
  placeholder or invented user id. Only endpoints meant to be genuinely public (anonymous
  browsing, like `/api/map/**`) should be added to `SecurityConfig`'s `permitAll` matcher
  list — that's a deliberate, explicit opt-in per route prefix, not a default.

## 7. Tests

Per `docs/testing.md`: Mockito unit tests for the service; `@WebMvcTest` for the controller
including one failure case asserting the error envelope (`isSuccess=false`, correct `code`).
No H2 — DB-dependent tests need the real PostGIS (docker compose).

## 8. Finish

- New query patterns → add `@Table(indexes = ...)` entries on the entity.
- Run the `verify` skill (DB up → build → bootRun → curl happy + failure paths).
- Commit as `[Feat] <한국어 요약>` (see `docs/conventions.md`).
