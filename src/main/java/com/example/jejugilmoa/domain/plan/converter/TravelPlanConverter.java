package com.example.jejugilmoa.domain.plan.converter;

import com.example.jejugilmoa.domain.plan.dto.*;
import com.example.jejugilmoa.domain.plan.entity.DayDeparture;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.user.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class TravelPlanConverter {

    private TravelPlanConverter() {}

    public static TravelPlan toEntity(User user, TravelPlanCreateRequest request) {
        long days = ChronoUnit.DAYS.between(request.startDate(), request.endDate());
        int totalAvailableTime = (int) (days + 1) * 8 * 60;

        return TravelPlan.builder()
                .user(user)
                .title(request.title())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .sameDay(request.startDate().equals(request.endDate()))
                .totalAvailableTime(totalAvailableTime)
                .companion(request.companion())
                .build();
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

    public static TravelPlanDetailResponse toDetail(TravelPlan plan, List<WaypointResponse> waypoints,
                                                    List<DayDeparture> dayDepartures) {
        int nights = (int) ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate());

        Map<LocalDate, List<WaypointResponse>> byDate = waypoints.stream()
                .collect(Collectors.groupingBy(WaypointResponse::visitDate, LinkedHashMap::new, Collectors.toList()));

        Map<LocalDate, DayDeparture> departureByDate = dayDepartures.stream()
                .collect(Collectors.toMap(DayDeparture::getVisitDate, d -> d));

        List<DayItineraryResponse> itinerary = IntStream.rangeClosed(0, nights)
                .mapToObj(i -> {
                    LocalDate date = plan.getStartDate().plusDays(i);
                    DayDeparture dep = departureByDate.get(date);
                    String depName = null;
                    BigDecimal depLat = null, depLon = null;
                    if (dep != null) {
                        depName = dep.getPlace() != null ? dep.getPlace().getName() : dep.getLocationName();
                        depLat = dep.getLatitude();
                        depLon = dep.getLongitude();
                    }
                    return new DayItineraryResponse(date, i + 1, depName, depLat, depLon,
                            byDate.getOrDefault(date, List.of()));
                })
                .toList();

        List<String> categories = plan.getPreferredCategories().stream()
                .map(pref -> pref.getTheme().name())
                .toList();

        boolean anyBudgetSet = plan.getBudgetTransportation() != null
                || plan.getBudgetAccommodation() != null
                || plan.getBudgetFood() != null
                || plan.getBudgetEtc() != null;
        Integer totalBudget = anyBudgetSet
                ? Stream.of(plan.getBudgetTransportation(), plan.getBudgetAccommodation(),
                            plan.getBudgetFood(), plan.getBudgetEtc())
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .sum()
                : null;

        return new TravelPlanDetailResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getStartDate(),
                plan.getEndDate(),
                nights,
                nights + 1,
                plan.getStatus(),
                plan.getTravelStyle(),
                plan.getCompanion(),
                categories,
                itinerary,
                plan.getBudgetTransportation(),
                plan.getBudgetAccommodation(),
                plan.getBudgetFood(),
                plan.getBudgetEtc(),
                totalBudget
        );
    }
}
