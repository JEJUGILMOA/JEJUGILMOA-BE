package com.example.jejugilmoa.domain.plan.dto;

import com.example.jejugilmoa.domain.plan.enums.TransportMode;
import com.example.jejugilmoa.domain.plan.enums.TravelRegion;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record TravelPlanCreateRequest(
        @NotBlank @Size(max = 200) String title,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate,
        @NotNull TravelRegion region,
        Long departurePlaceId,
        @Size(max = 200) String departureLocationName,
        Long destinationPlaceId,
        @Size(max = 200) String destinationLocationName,
        @NotNull TransportMode transportMode,
        @NotNull @Size(min = 1) List<Long> categoryIds
) {}
