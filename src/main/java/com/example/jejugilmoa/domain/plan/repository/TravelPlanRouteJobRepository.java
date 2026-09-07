package com.example.jejugilmoa.domain.plan.repository;

import com.example.jejugilmoa.domain.plan.dto.TravelPlanRouteJobClaim;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class TravelPlanRouteJobRepository {
    private final JdbcClient jdbc;

    public void enqueue(Long planId) {
        jdbc.sql("""
                INSERT INTO travel_plan_route_update_job
                    (plan_id, status, dirty, attempt_count, next_attempt_at, created_at, updated_at)
                VALUES (:planId, 'PENDING', true, 0, clock_timestamp(), clock_timestamp(), clock_timestamp())
                ON CONFLICT (plan_id) DO UPDATE SET
                    dirty = true,
                    status = CASE WHEN travel_plan_route_update_job.status = 'DONE' THEN 'PENDING'
                                  ELSE travel_plan_route_update_job.status END,
                    attempt_count = CASE WHEN travel_plan_route_update_job.status = 'DONE' THEN 0
                                         ELSE travel_plan_route_update_job.attempt_count END,
                    next_attempt_at = CASE WHEN travel_plan_route_update_job.status = 'DONE' THEN clock_timestamp()
                                           ELSE travel_plan_route_update_job.next_attempt_at END,
                    updated_at = clock_timestamp()
                """).param("planId", planId).update();
    }

    public Optional<TravelPlanRouteJobClaim> claim(int leaseSeconds) {
        UUID token = UUID.randomUUID();
        return jdbc.sql("""
                WITH candidate AS (
                    SELECT id FROM travel_plan_route_update_job
                    WHERE (status = 'PENDING' AND next_attempt_at <= clock_timestamp())
                       OR (status = 'RUNNING' AND lease_until <= clock_timestamp())
                    ORDER BY next_attempt_at, id
                    FOR UPDATE SKIP LOCKED LIMIT 1
                )
                UPDATE travel_plan_route_update_job j
                SET status = 'RUNNING', dirty = false, lease_token = :token,
                    lease_until = clock_timestamp() + make_interval(secs => :lease),
                    attempt_count = LEAST(attempt_count + 1, 30), updated_at = clock_timestamp()
                FROM candidate c WHERE j.id = c.id
                RETURNING j.id, j.plan_id, j.attempt_count
                """).param("token", token).param("lease", leaseSeconds)
                .query((rs, row) -> new TravelPlanRouteJobClaim(rs.getLong("id"), rs.getLong("plan_id"),
                        token, rs.getInt("attempt_count"))).optional();
    }

    public boolean renew(TravelPlanRouteJobClaim claim, int leaseSeconds) {
        return jdbc.sql("""
                UPDATE travel_plan_route_update_job
                SET lease_until = clock_timestamp() + make_interval(secs => :lease), updated_at = clock_timestamp()
                WHERE id = :id AND plan_id = :planId AND status = 'RUNNING'
                  AND lease_token = :token AND lease_until > clock_timestamp()
                """).param("id", claim.id()).param("planId", claim.planId()).param("token", claim.token())
                .param("lease", leaseSeconds).update() == 1;
    }

    public boolean lockOwned(TravelPlanRouteJobClaim claim) {
        return jdbc.sql("""
                SELECT id FROM travel_plan_route_update_job
                WHERE id = :id AND plan_id = :planId AND status = 'RUNNING'
                  AND lease_token = :token AND lease_until > clock_timestamp()
                FOR UPDATE
                """).param("id", claim.id()).param("planId", claim.planId()).param("token", claim.token())
                .query(Long.class).optional().isPresent();
    }

    public void finish(TravelPlanRouteJobClaim claim, boolean success, int retrySeconds, String error) {
        jdbc.sql("""
                UPDATE travel_plan_route_update_job
                SET status = CASE WHEN :success AND NOT dirty THEN 'DONE' ELSE 'PENDING' END,
                    next_attempt_at = clock_timestamp() + make_interval(secs => :delay),
                    attempt_count = CASE WHEN :success THEN 0 ELSE attempt_count END,
                    lease_until = NULL, lease_token = NULL, last_error = :error, updated_at = clock_timestamp()
                WHERE id = :id AND plan_id = :planId AND status = 'RUNNING'
                  AND lease_token = :token AND lease_until > clock_timestamp()
                """).param("success", success).param("delay", success ? 0 : retrySeconds)
                .param("error", error, java.sql.Types.VARCHAR).param("id", claim.id())
                .param("planId", claim.planId()).param("token", claim.token()).update();
    }
}
