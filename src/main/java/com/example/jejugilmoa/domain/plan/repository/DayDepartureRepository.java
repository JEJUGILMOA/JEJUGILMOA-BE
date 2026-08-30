package com.example.jejugilmoa.domain.plan.repository;

import com.example.jejugilmoa.domain.plan.entity.DayDeparture;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DayDepartureRepository extends JpaRepository<DayDeparture, Long> {
    List<DayDeparture> findAllByTravelPlanIdOrderByVisitDateAsc(Long planId);
}
