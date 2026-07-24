package com.example.jejugilmoa.domain.plan.repository;

import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {

    @Query("SELECT p FROM TravelPlan p WHERE p.user.id = :userId AND p.user.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<TravelPlan> findMyPlans(@Param("userId") Long userId);

    @Query("SELECT p FROM TravelPlan p WHERE p.user.id = :userId AND p.user.deletedAt IS NULL AND p.status = :status ORDER BY p.createdAt DESC")
    List<TravelPlan> findMyPlansByStatus(@Param("userId") Long userId, @Param("status") TravelPlanStatus status);

    @Query("SELECT p.id, COUNT(c) FROM TravelPlan p LEFT JOIN p.travelCourses c WHERE p.id IN :planIds GROUP BY p.id")
    List<Object[]> countCoursesByPlanIds(@Param("planIds") List<Long> planIds);
}
