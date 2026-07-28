package com.example.jejugilmoa.domain.plan.repository;

import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TravelCourseRepository extends JpaRepository<TravelCourse, Long> {

    List<TravelCourse> findAllByTravelPlanIdOrderBySequenceOrderAsc(Long planId);

    boolean existsByTravelPlanIdAndPlaceId(Long planId, Long placeId);

    int countByTravelPlanId(Long planId);

    // 단일 UPDATE로 uk_course_plan_sequence 임시 중복 없이 순번을 앞당김
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE TravelCourse c SET c.sequenceOrder = c.sequenceOrder - 1 WHERE c.travelPlan.id = :planId AND c.sequenceOrder > :removedOrder")
    void decrementSequenceOrderAfter(@Param("planId") Long planId, @Param("removedOrder") int removedOrder);

    // 순서 변경 Phase-1: 모든 순번을 +offset으로 임시 이동 (uk_course_plan_sequence 충돌 방지)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("UPDATE TravelCourse c SET c.sequenceOrder = c.sequenceOrder + :offset WHERE c.travelPlan.id = :planId")
    void shiftSequenceOrderByOffset(@Param("planId") Long planId, @Param("offset") int offset);

    // 순서 변경 Phase-2: 특정 경유지의 순번을 최종값으로 설정
    @Modifying
    @Query("UPDATE TravelCourse c SET c.sequenceOrder = :newOrder WHERE c.id = :waypointId AND c.travelPlan.id = :planId")
    void updateSequenceOrder(@Param("waypointId") Long waypointId, @Param("planId") Long planId, @Param("newOrder") int newOrder);
}
