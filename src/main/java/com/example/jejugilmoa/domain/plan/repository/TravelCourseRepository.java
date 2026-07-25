package com.example.jejugilmoa.domain.plan.repository;

import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TravelCourseRepository extends JpaRepository<TravelCourse, Long> {

    List<TravelCourse> findAllByTravelPlanIdOrderBySequenceOrderAsc(Long planId);

    boolean existsByTravelPlanIdAndPlaceId(Long planId, Long placeId);

    int countByTravelPlanId(Long planId);
}
