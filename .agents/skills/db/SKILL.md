---
name: db
description: Operate the local PostGIS database — start/stop/reset, inspect schema, run SQL, debug spatial queries. Use for any local database task in this project.
---

# Local database operations

The app requires PostGIS (geometry columns) — plain postgres or H2 will not work.
Defaults: db `jejugilmoa`, user/pass `postgres`/`postgres`, port 5432 (see `.env.example`).

**Port conflicts**: if 5432 is taken (another project's postgres), put `DB_PORT=5433` in a
root `.env` file. docker compose reads `.env` automatically; **Spring/Gradle do NOT** — pass
it explicitly: `DB_PORT=5433 ./gradlew build|bootRun|test`. Check the current mapping with
`docker compose ps` before assuming the port.

## Lifecycle

```bash
docker compose up -d          # start (idempotent)
docker compose ps             # health status
docker compose logs -f db     # logs
docker compose down           # stop, keep data
docker compose down -v        # DESTROY data — dev ddl-auto:update recreates schema on next boot
```

Reset (`down -v`) is the fix for schema drift: `ddl-auto: update` never drops or renames
columns, so entity renames leave stale columns behind.

## Inspect / query

```bash
docker compose exec db psql -U postgres -d jejugilmoa -c "\dt"            # tables
docker compose exec db psql -U postgres -d jejugilmoa -c "\d travel_plan" # table schema
docker compose exec db psql -U postgres -d jejugilmoa -c "SELECT ..."     # arbitrary SQL
```

For multi-statement work, open interactive-free sessions with `-c` per statement or
`psql -f` with a mounted file — never rely on interactive psql.

## Spatial debugging

```sql
SELECT postgis_version();                          -- extension is preinstalled in this image
SELECT id, name, ST_AsText(geom) FROM place;       -- read geometry as WKT
-- radius search (the app's core query shape; geography cast = meters):
SELECT id, name FROM place
WHERE ST_DWithin(geom, ST_SetSRID(ST_MakePoint(126.5312, 33.4996), 4326)::geography, 3000);
```

Gotchas:
- `ST_MakePoint(lng, lat)` — **longitude first**. Swapped arguments produce empty results,
  not errors.
- `geom` and `latitude`/`longitude` are stored redundantly and must agree
  (`docs/adr/0002-postgis-dual-storage.md`).
- `user` is a reserved word: quote it — `SELECT * FROM "user";`
