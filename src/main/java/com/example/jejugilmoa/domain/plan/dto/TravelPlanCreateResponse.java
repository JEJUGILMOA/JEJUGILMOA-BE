package com.example.jejugilmoa.domain.plan.dto;

import com.example.jejugilmoa.domain.plan.enums.TransportMode;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import com.example.jejugilmoa.domain.plan.enums.TravelRegion;

import java.time.LocalDate;
import java.util.List;

public record TravelPlanCreateResponse(
        Long planId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        TravelRegion region,
        TransportMode transportMode,
        TravelPlanStatus status,
        List<String> categories
) {}
