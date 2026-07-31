package com.example.jejugilmoa.domain.plan.converter;

import com.example.jejugilmoa.domain.place.entity.Category;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.plan.dto.*;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.user.entity.User;

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
        String departureName = plan.getDeparturePlace() != null
                ? plan.getDeparturePlace().getName()
                : plan.getDepartureLocationName();
        String destinationName = plan.getDestinationPlace() != null
                ? plan.getDestinationPlace().getName()
                : plan.getDestinationLocationName();

        return new TravelPlanCreateResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getStartDate(),
                plan.getEndDate(),
                plan.getRegion(),
                plan.getTransportMode(),
                plan.getStatus(),
                categories.stream().map(Category::getName).toList(),
                departureName,
                destinationName
        );
    }

    public static BudgetUpdateResponse toBudgetResponse(TravelPlan plan) {
        boolean anySet = plan.getBudgetTransportation() != null
                || plan.getBudgetAccommodation() != null
                || plan.getBudgetFood() != null
                || plan.getBudgetEtc() != null;
        Integer total = anySet
                ? Stream.of(plan.getBudgetTransportation(), plan.getBudgetAccommodation(),
                            plan.getBudgetFood(), plan.getBudgetEtc())
                        .filter(Objects::nonNull)
                        .mapToInt(Integer::intValue)
                        .sum()
                : null;

        return new BudgetUpdateResponse(
                plan.getId(),
                plan.getBudgetTransportation(),
                plan.getBudgetAccommodation(),
                plan.getBudgetFood(),
                plan.getBudgetEtc(),
                total
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

    public static TravelPlanDetailResponse toDetail(TravelPlan plan, List<WaypointResponse> waypoints) {
        int nights = (int) ChronoUnit.DAYS.between(plan.getStartDate(), plan.getEndDate());

        Map<LocalDate, List<WaypointResponse>> byDate = waypoints.stream()
                .collect(Collectors.groupingBy(WaypointResponse::visitDate, LinkedHashMap::new, Collectors.toList()));

        // 계획 기간의 모든 날짜를 포함 (경유지 없는 날도 빈 리스트로)
        List<DayItineraryResponse> itinerary = IntStream.rangeClosed(0, nights)
                .mapToObj(i -> {
                    LocalDate date = plan.getStartDate().plusDays(i);
                    return new DayItineraryResponse(date, i + 1, byDate.getOrDefault(date, List.of()));
                })
                .toList();

        List<String> categories = plan.getPreferredCategories().stream()
                .map(pref -> pref.getCategory().getName())
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

        String departureName = plan.getDeparturePlace() != null
                ? plan.getDeparturePlace().getName()
                : plan.getDepartureLocationName();
        String destinationName = plan.getDestinationPlace() != null
                ? plan.getDestinationPlace().getName()
                : plan.getDestinationLocationName();

        return new TravelPlanDetailResponse(
                plan.getId(),
                plan.getTitle(),
                plan.getStartDate(),
                plan.getEndDate(),
                nights,
                nights + 1,
                plan.getStatus(),
                plan.getRegion(),
                plan.getTransportMode(),
                plan.getTravelStyle(),
                departureName,
                destinationName,
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
