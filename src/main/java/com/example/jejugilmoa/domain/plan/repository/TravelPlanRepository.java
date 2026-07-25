package com.example.jejugilmoa.domain.plan.repository;

import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import java.util.List;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {

    @Query("SELECT p FROM TravelPlan p WHERE p.user.id = :userId AND p.user.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<TravelPlan> findMyPlans(@Param("userId") Long userId);

    @Query("SELECT p FROM TravelPlan p WHERE p.user.id = :userId AND p.user.deletedAt IS NULL AND p.status = :status ORDER BY p.createdAt DESC")
    List<TravelPlan> findMyPlansByStatus(@Param("userId") Long userId, @Param("status") TravelPlanStatus status);

    @Query("SELECT p.id, COUNT(c) FROM TravelPlan p LEFT JOIN p.travelCourses c WHERE p.id IN :planIds GROUP BY p.id")
    List<Object[]> countCoursesByPlanIds(@Param("planIds") List<Long> planIds);

    /**
     * 선호 카테고리(preferredCategories → category)를 한 번의 쿼리로 페치해
     * 추천 로직에서 N+1 없이 카테고리 ID를 읽을 수 있도록 합니다.
     */
    @Query("""
        SELECT p FROM TravelPlan p
                    JOIN p.user u
        LEFT JOIN FETCH p.preferredCategories pc
        LEFT JOIN FETCH pc.category
        WHERE p.id = :planId
                    AND u.deletedAt IS NULL
        """)
    Optional<TravelPlan> findByIdWithPreferences(@Param("planId") Long planId);

}
