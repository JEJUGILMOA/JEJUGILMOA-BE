package com.example.jejugilmoa.domain.plan.converter;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.plan.dto.ShareLinkResponse;
import com.example.jejugilmoa.domain.plan.dto.SharedPlanResponse;
import com.example.jejugilmoa.domain.plan.entity.DayDeparture;
import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.entity.TravelSharedPlan;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class PlanShareConverter {

    private PlanShareConverter() {}

    public static ShareLinkResponse toShareLinkResponse(TravelSharedPlan sharedPlan) {
        return new ShareLinkResponse(
                sharedPlan.getTravelPlan().getId(),
                sharedPlan.getShareToken(),
                sharedPlan.getExpiresAt());
    }

    public static SharedPlanResponse toSharedPlanResponse(TravelPlan plan, List<TravelCourse> courses,
                                                          List<DayDeparture> dayDepartures) {
        List<SharedPlanResponse.SharedWaypointResponse> waypoints = courses.stream()
                .map(course -> {
                    Place place = course.getPlace();
                    return new SharedPlanResponse.SharedWaypointResponse(
                            course.getVisitDate(), course.getSequenceOrder(), place.getName(), place.getAddress(),
                            place.getLatitude(), place.getLongitude(), place.getImageUrl());
                })
                .toList();

        // 출발지는 여행 시작일의 DayDeparture를 대표값으로 사용
        String departure = dayDepartures.stream()
                .filter(d -> d.getVisitDate().equals(plan.getStartDate()))
                .findFirst()
                .map(d -> d.getPlace() != null ? d.getPlace().getName() : d.getLocationName())
                .orElse(null);

        boolean anyBudgetSet = plan.getBudgetTransportation() != null
                || plan.getBudgetAccommodation() != null
                || plan.getBudgetFood() != null
                || plan.getBudgetEtc() != null;
        Integer totalBudget = anyBudgetSet
                ? Stream.of(plan.getBudgetTransportation(), plan.getBudgetAccommodation(),
                            plan.getBudgetFood(), plan.getBudgetEtc())
                        .filter(Objects::nonNull).mapToInt(Integer::intValue).sum()
                : null;

        return new SharedPlanResponse(
                plan.getId(), plan.getTitle(), plan.getStartDate(), plan.getEndDate(), departure,
                waypoints, plan.getBudgetTransportation(), plan.getBudgetAccommodation(), plan.getBudgetFood(),
                plan.getBudgetEtc(), totalBudget);
    }
}
