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

- Spatial queries must be native (see `docs/adr/0002-postgis-dual-storage.md`):
  ```java
  @Query(value = """
      SELECT * FROM place p
      WHERE p.is_published = true
        AND ST_DWithin(p.geom, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radiusMeters)
      """, nativeQuery = true)
  List<Place> findWithinRadius(double lat, double lng, double radiusMeters);
  ```
- Soft-deleted entities (`User`, `TravelRecord`): every query must filter `deletedAt IS NULL`
  (e.g. `findByIdAndDeletedAtIsNull`). See `docs/adr/0003-soft-delete.md`.

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

Static methods only; keeps mapping out of services.

```java
public class TravelPlanConverter {
    public static TravelPlan toEntity(TravelPlanCreateRequest req, User user) { ... }
    public static TravelPlanResponse toResponse(TravelPlan plan) { ... }
}
```

## 5. Service — `domain/<x>/service/`

```java
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TravelPlanService {
    private final TravelPlanRepository travelPlanRepository;

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
        return TravelPlanConverter.toResponse(plan);
    }
}
```

Business failures always via `throw new GeneralException(<code>)` — never try-catch in
controllers, never return null/Optional to signal failure across the service boundary.

## 6. Controller — `domain/<x>/controller/`

```java
@Tag(name = "여행 계획", description = "여행 계획 API")
@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class TravelPlanController {
    private final TravelPlanService travelPlanService;

    @Operation(summary = "여행 계획 생성")
    @PostMapping
    public ApiResponse<TravelPlanResponse> create(@Valid @RequestBody TravelPlanCreateRequest request) {
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED,
                travelPlanService.create(request, /* userId: auth 미구현 — TODO */ null));
    }
}
```

- Route prefix: `/api/v1/<plural-resource>`.
- Swagger annotations (`@Tag`, `@Operation`) on everything — Swagger UI is the team's
  primary API contract surface.
- Auth is not implemented yet (`SecurityConfig` permits all). Where a userId is needed,
  leave an explicit `TODO` — do not invent a security context.

## 7. Tests

Per `docs/testing.md`: Mockito unit tests for the service; `@WebMvcTest` for the controller
including one failure case asserting the error envelope (`isSuccess=false`, correct `code`).
No H2 — DB-dependent tests need the real PostGIS (docker compose).

## 8. Finish

- New query patterns → add `@Table(indexes = ...)` entries on the entity.
- Run the `verify` skill (DB up → build → bootRun → curl happy + failure paths).
- Commit as `[Feat] <한국어 요약>` (see `docs/conventions.md`).
