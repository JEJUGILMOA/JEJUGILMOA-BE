---
name: new-entity
description: Create or modify a JPA entity following this repo's entity conventions (builder pattern, no setters, auditing, explicit indexes, STRING enums). Use whenever adding an entity, adding fields, or changing relationships.
---

# Entity conventions checklist

Reference implementations: `TravelPlan` (typical), `Report` (state-transition methods),
`Place` (spatial). Full rules: `docs/conventions.md`, `docs/architecture.md`.

## Class shape (mandatory)

```java
@Entity
@Table(name = "travel_plan", indexes = {
        @Index(name = "idx_plan_user_status", columnList = "user_id,status")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class TravelPlan extends BaseEntity { ... }
```

- Always extend `BaseEntity` (provides audited `createdAt`/`updatedAt` as `Instant`).
- **No `@Setter`.** Mutations go through intention-revealing methods on the entity
  (model: `Report.approve()`/`reject()`/`resolve()`).
- `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)` — IDENTITY, not SEQUENCE/AUTO.
- Fields with builder defaults need `@Builder.Default`.

## Columns

- Table names: snake_case singular. `user` is a Postgres reserved word — quoted as `` "`user`" ``.
- Enums: `@Enumerated(EnumType.STRING)` + explicit `length`. Enum type lives in
  `domain/<x>/enums/`.
- Strings: explicit `length`; long text → `columnDefinition = "TEXT"`.
- Booleans: Java field without `is` prefix, column with it:
  `@Column(name = "is_published") private boolean published;`
- Korean comment on non-obvious fields (constraint/why, not what).

## Relationships

- `@ManyToOne(fetch = FetchType.LAZY)` always — never EAGER.
- Owning side declares `@JoinColumn(name = "<x>_id")`; parent side uses
  `mappedBy` + `cascade = CascadeType.ALL, orphanRemoval = true` only when children's
  lifecycle is fully owned by the parent.
- Collections initialized inline: `@Builder.Default private List<X> xs = new ArrayList<>();`
- Cross-aggregate "references" that must survive target deletion use `targetType + targetId`
  (no FK) — see `Report`.

## Special cases

- **Soft delete** (`docs/adr/0003-soft-delete.md`): nullable `LocalDateTime deletedAt`,
  set via a `delete()` method on the entity. All queries on the entity must filter it.
- **Coordinates** (`docs/adr/0002-postgis-dual-storage.md`): if the entity stores a location,
  store BOTH `latitude`/`longitude` (`BigDecimal`) and PostGIS `geom`
  (`@JdbcTypeCode(SqlTypes.GEOMETRY)`, `geometry(Point,4326)`), and encapsulate their
  joint update in one entity method. `ST_MakePoint` takes **longitude first**.

## After changing any entity

1. Add/adjust `@Table(indexes = ...)` for the query patterns the change serves.
2. Update the ERD in `docs/architecture.md` (PR checklist item).
3. Verify the generated schema (dev profile auto-applies via `ddl-auto: update`):
   ```bash
   docker compose exec db psql -U postgres -d jejugilmoa -c "\d <table_name>"
   ```
   Note: `ddl-auto: update` never DROPS columns — renames leave the old column behind
   in existing local DBs. `docker compose down -v && docker compose up -d` for a clean slate.
