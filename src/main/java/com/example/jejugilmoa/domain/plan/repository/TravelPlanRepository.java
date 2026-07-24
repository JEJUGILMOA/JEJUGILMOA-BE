package com.example.jejugilmoa.domain.plan.repository;

import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {

    List<TravelPlan> findByUser_IdOrderByCreatedAtDesc(Long userId);

    List<TravelPlan> findByUser_IdAndStatusOrderByCreatedAtDesc(Long userId, TravelPlanStatus status);
}
