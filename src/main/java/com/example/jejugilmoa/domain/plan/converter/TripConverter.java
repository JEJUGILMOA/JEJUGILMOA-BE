package com.example.jejugilmoa.domain.plan.converter;

import com.example.jejugilmoa.domain.badge.converter.BadgeConverter;
import com.example.jejugilmoa.domain.badge.dto.BadgeEarnedResponse;
import com.example.jejugilmoa.domain.plan.dto.TripCancelResponse;
import com.example.jejugilmoa.domain.plan.dto.TripCompleteResponse;
import com.example.jejugilmoa.domain.plan.dto.TripResponse;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanRoutesResponse;
import com.example.jejugilmoa.domain.plan.dto.VisitCheckResponse;
import com.example.jejugilmoa.domain.plan.dto.WaypointResponse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.user.entity.UserBadge;

import java.time.temporal.ChronoUnit;
import java.util.List;

public class TripConverter {

    private TripConverter() {}

    public static TripResponse toResponse(TravelPlan plan, List<WaypointResponse> waypoints,
            TravelPlanRoutesResponse routes) {
        return new TripResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getStatus(),
                plan.getActualStartedAt(),
                waypoints,
                routes
        );
    }

    public static TripCancelResponse toCancelResponse(TravelPlan plan) {
        return new TripCancelResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getStatus(),
                plan.getActualStartedAt(),
                plan.getActualCancelledAt()
        );
    }

    public static VisitCheckResponse toVisitCheckResponse(
            List<WaypointResponse> waypoints, boolean autoCompleted, List<UserBadge> earnedBadges) {
        List<BadgeEarnedResponse> badges = earnedBadges == null ? null
                : earnedBadges.stream().map(BadgeConverter::toEarnedResponse).toList();
        return new VisitCheckResponse(waypoints, autoCompleted, badges);
    }

    public static TripCompleteResponse toCompleteResponse(
            TravelPlan plan, int placeCount, double totalDistanceKm, List<UserBadge> earnedBadges) {
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
                plan.getActualCompletedAt(),
                earnedBadges.stream().map(BadgeConverter::toEarnedResponse).toList()
        );
    }
}
