package com.example.jejugilmoa.domain.plan.repository;

import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {

    interface RouteAccess {
        Long getPlanId();
        Long getOwnerId();
    }

    @Query("""
            SELECT p.id AS planId, u.id AS ownerId FROM TravelPlan p
            JOIN p.user u
            WHERE p.id = :planId AND u.deletedAt IS NULL
            """)
    Optional<RouteAccess> findRouteAccessById(@Param("planId") Long planId);

    @Query("SELECT p FROM TravelPlan p WHERE p.user.id = :userId AND p.user.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<TravelPlan> findMyPlans(@Param("userId") Long userId);

    @Query("SELECT p FROM TravelPlan p WHERE p.user.id = :userId AND p.user.deletedAt IS NULL AND p.status = :status ORDER BY p.createdAt DESC")
    List<TravelPlan> findMyPlansByStatus(@Param("userId") Long userId, @Param("status") TravelPlanStatus status);

    // 유저당 진행중(IN_PROGRESS) 여행은 최대 1건이라는 비즈니스 규칙을 전제로 함
    @Query("SELECT p FROM TravelPlan p WHERE p.user.id = :userId AND p.user.deletedAt IS NULL AND p.status = :status")
    Optional<TravelPlan> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") TravelPlanStatus status);

    @Query("SELECT p.id, COUNT(c) FROM TravelPlan p LEFT JOIN p.travelCourses c WHERE p.id IN :planIds GROUP BY p.id")
    List<Object[]> countCoursesByPlanIds(@Param("planIds") List<Long> planIds);

    // 재방문형 뱃지(완료한 여행 N회) 판정용 — GPS 인증 방문(skipped=false)이 하나 이상인 완료 여행만 집계
    @Query("""
            SELECT COUNT(DISTINCT p) FROM TravelPlan p
            JOIN p.travelCourses c
            WHERE p.user.id = :userId
            AND p.user.deletedAt IS NULL
            AND p.status = :status
            AND c.visited = true
            AND c.skipped = false
            """)
    long countCompletedTripsWithGpsVisit(@Param("userId") Long userId, @Param("status") TravelPlanStatus status);

    // 경유지 추가/삭제 시 순번 충돌 방지용 — 같은 plan에 대한 쓰기 요청을 직렬화
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM TravelPlan p WHERE p.id = :planId")
    Optional<TravelPlan> findByIdForUpdate(@Param("planId") Long planId);

    @Query("""
        SELECT p FROM TravelPlan p
                    JOIN p.user u
        LEFT JOIN FETCH p.preferredCategories
        WHERE p.id = :planId
                    AND u.deletedAt IS NULL
        """)
    Optional<TravelPlan> findByIdWithPreferences(@Param("planId") Long planId);

    // 자동 시작 대상: 시작일이 오늘 이하이고, DRAFT 상태이며, 해당 유저에게 이미 IN_PROGRESS 계획이 없는 것
    @Query("""
            SELECT p.id FROM TravelPlan p
            WHERE p.status = com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus.DRAFT
            AND p.startDate <= :today
            AND p.user.deletedAt IS NULL
            AND NOT EXISTS (
                SELECT 1 FROM TravelPlan other
                WHERE other.user.id = p.user.id
                AND other.status = com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus.IN_PROGRESS
            )
            """)
    List<Long> findDraftPlanIdsToAutoStart(@Param("today") LocalDate today);

}
