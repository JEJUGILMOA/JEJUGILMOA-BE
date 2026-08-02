package com.example.jejugilmoa.domain.plan.converter;

import com.example.jejugilmoa.domain.plan.dto.TripResponse;
import com.example.jejugilmoa.domain.plan.dto.WaypointResponse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;

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
}
