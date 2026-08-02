package com.example.jejugilmoa.domain.plan.repository;

import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TravelCourseRepository extends JpaRepository<TravelCourse, Long> {

    List<TravelCourse> findAllByTravelPlanIdOrderBySequenceOrderAsc(Long planId);

    // 방문 체크 순서 검증용 — sequenceOrder가 가장 빠른 미방문 경유지 (= 다음에 체크해야 할 경유지)
    Optional<TravelCourse> findFirstByTravelPlanIdAndVisitedFalseOrderBySequenceOrderAsc(Long planId);

    boolean existsByTravelPlanIdAndPlaceId(Long planId, Long placeId);

    // visitDate 기준 오름차순 정렬 — 목록 조회용
    List<TravelCourse> findAllByTravelPlanIdOrderByVisitDateAscSequenceOrderAsc(Long planId);

    // 특정 Day의 경유지만 조회 — 재정렬 유효성 검사용
    List<TravelCourse> findAllByTravelPlanIdAndVisitDateOrderBySequenceOrderAsc(Long planId, LocalDate visitDate);

    // 특정 Day의 경유지 수 — addWaypoint 시 nextOrder 계산용
    int countByTravelPlanIdAndVisitDate(Long planId, LocalDate visitDate);

    // removeWaypoint: 같은 Day에서 제거된 순번 이후 순번을 1씩 당김
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE TravelCourse c
            SET c.sequenceOrder = c.sequenceOrder - 1
            WHERE c.travelPlan.id = :planId
              AND c.visitDate = :visitDate
              AND c.sequenceOrder > :removedOrder
            """)
    void decrementSequenceOrderAfter(
            @Param("planId") Long planId,
            @Param("visitDate") LocalDate visitDate,
            @Param("removedOrder") int removedOrder);

    // reorderWaypoints Phase-1: 같은 Day의 순번을 임시 offset으로 이동 (uk_course_plan_date_sequence 충돌 방지)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE TravelCourse c
            SET c.sequenceOrder = c.sequenceOrder + :offset
            WHERE c.travelPlan.id = :planId
              AND c.visitDate = :visitDate
            """)
    void shiftSequenceOrderByOffset(
            @Param("planId") Long planId,
            @Param("visitDate") LocalDate visitDate,
            @Param("offset") int offset);

    // reorderWaypoints Phase-2: 특정 경유지의 순번을 최종값으로 확정
    @Modifying
    @Query("""
            UPDATE TravelCourse c
            SET c.sequenceOrder = :newOrder
            WHERE c.id = :waypointId
              AND c.travelPlan.id = :planId
            """)
    void updateSequenceOrder(
            @Param("waypointId") Long waypointId,
            @Param("planId") Long planId,
            @Param("newOrder") int newOrder);
}
