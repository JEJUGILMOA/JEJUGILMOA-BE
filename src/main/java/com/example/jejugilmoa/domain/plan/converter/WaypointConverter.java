package com.example.jejugilmoa.domain.plan.converter;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.plan.dto.WaypointResponse;
import com.example.jejugilmoa.domain.plan.entity.TravelCourse;

public class WaypointConverter {

    private WaypointConverter() {
    }

    public static WaypointResponse toResponse(TravelCourse course) {
        Place place = course.getPlace();
        return new WaypointResponse(
            course.getId(),
            course.getVisitDate(),
            course.getSequenceOrder(),
            place.getId(),
            place.getName(),
            place.getCategory().getName(),
            place.getImageUrl(),
            place.getAddress(),
            course.isVisited(),
            course.getVisitedAt()
        );
    }
}
