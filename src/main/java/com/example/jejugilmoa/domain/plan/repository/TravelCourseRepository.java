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
}
