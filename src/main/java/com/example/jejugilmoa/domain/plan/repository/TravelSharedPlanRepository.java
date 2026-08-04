package com.example.jejugilmoa.domain.plan.repository;

import com.example.jejugilmoa.domain.plan.entity.TravelSharedPlan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelSharedPlanRepository extends JpaRepository<TravelSharedPlan, Long> {

    Optional<TravelSharedPlan> findByTravelPlanId(Long planId);

    Optional<TravelSharedPlan> findByShareToken(String shareToken);

    boolean existsByShareToken(String shareToken);
}
