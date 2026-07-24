package com.example.jejugilmoa.domain.plan.converter;

import com.example.jejugilmoa.domain.place.entity.Category;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanCreateRequest;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanCreateResponse;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanListResponse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.user.entity.User;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class TravelPlanConverter {

    private TravelPlanConverter() {}

    public static TravelPlan toEntity(User user, Place departurePlace, Place destinationPlace,
                                      TravelPlanCreateRequest request) {
        long days = ChronoUnit.DAYS.between(request.startDate(), request.endDate());
        int totalAvailableTime = (int) (days + 1) * 8 * 60;

        return TravelPlan.builder()
                .user(user)
                .title(request.title())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .sameDay(request.startDate().equals(request.endDate()))
                .totalAvailableTime(totalAvailableTime)
                .transportMode(request.transportMode())
                .region(request.region())
                .departurePlace(departurePlace)
                .departureLocationName(request.departureLocationName())
                .destinationPlace(destinationPlace)
                .destinationLocationName(request.destinationLocationName())
                .build();
    }

    public static TravelPlanCreateResponse toCreateResponse(TravelPlan plan, List<Category> categories) {
        return new TravelPlanCreateResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getRegion(),
                plan.getTransportMode(),
                plan.getStatus(),
                categories.stream().map(Category::getName).toList()
        );
    }

    public static TravelPlanListResponse toSummary(TravelPlan plan, int waypointCount) {
        int nights = (int) ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate());
        int dDay   = (int) ChronoUnit.DAYS.between(LocalDate.now(), plan.getStartDate());

        return new TravelPlanListResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getStatus(),
                waypointCount,
                nights,
                nights + 1,
                dDay
        );
    }
}
