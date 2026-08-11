package com.example.jejugilmoa.domain.plan.converter;

import com.example.jejugilmoa.domain.plan.dto.TripCompleteResponse;
import com.example.jejugilmoa.domain.plan.dto.TripResponse;
import com.example.jejugilmoa.domain.plan.dto.WaypointResponse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;

import java.time.temporal.ChronoUnit;
import java.util.List;

public class TripConverter {

    private TripConverter() {}

    public static TripResponse toResponse(TravelPlan plan, List<WaypointResponse> waypoints) {
        return new TripResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getStatus(),
                plan.getActualStartedAt(),
                waypoints
        );
    }

    public static TripCompleteResponse toCompleteResponse(TravelPlan plan, int placeCount, double totalDistanceKm) {
        int durationDays = (int) (ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate()) + 1);
        return new TripCompleteResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getStatus(),
                plan.getStartDate(),
                plan.getEndDate(),
                durationDays,
                placeCount,
                Math.round(totalDistanceKm * 10) / 10.0,
                plan.getActualStartedAt(),
                plan.getActualCompletedAt()
        );
    }
}
