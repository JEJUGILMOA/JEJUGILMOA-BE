package com.example.jejugilmoa.domain.plan.converter;

import com.example.jejugilmoa.domain.direction.dto.DirectionResponse;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanRoutesResponse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlanRoute;
import java.util.List;

public class TravelPlanRouteConverter {
    private TravelPlanRouteConverter() {}

    public static List<List<Double>> toStoredPath(DirectionResponse response) {
        return response.path().stream().map(p -> List.of(p.lng(), p.lat())).toList();
    }

    public static TravelPlanRoutesResponse.Route toResponse(TravelPlanRoute route) {
        return new TravelPlanRoutesResponse.Route(route.getRouteDate(), route.getStatus(),
                route.getRouteOption(), route.getTotalDistance(), route.getTotalDuration(),
                route.getCalculatedAt(), route.getPath() == null ? List.of() : route.getPath(),
                route.getFailureCode());
    }
}
