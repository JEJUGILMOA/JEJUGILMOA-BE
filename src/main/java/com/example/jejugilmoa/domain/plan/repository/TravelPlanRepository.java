package com.example.jejugilmoa.domain.plan.repository;

import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {

    /**
     * 선호 카테고리(preferredCategories → category)를 한 번의 쿼리로 페치해
     * 추천 로직에서 N+1 없이 카테고리 ID를 읽을 수 있도록 합니다.
     */
    @Query("""
            SELECT p FROM TravelPlan p
            LEFT JOIN FETCH p.preferredCategories pc
            LEFT JOIN FETCH pc.category
            WHERE p.id = :planId
            """)
    Optional<TravelPlan> findByIdWithPreferences(@Param("planId") Long planId);
}
