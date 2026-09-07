package com.example.jejugilmoa.domain.plan.repository;

import com.example.jejugilmoa.domain.plan.entity.TravelPlanRoute;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TravelPlanRouteRepository extends JpaRepository<TravelPlanRoute, Long> {
    List<TravelPlanRoute> findAllByTravelPlanIdOrderByRouteDateAsc(Long planId);
    Optional<TravelPlanRoute> findByTravelPlanIdAndRouteDate(Long planId, LocalDate date);
}
