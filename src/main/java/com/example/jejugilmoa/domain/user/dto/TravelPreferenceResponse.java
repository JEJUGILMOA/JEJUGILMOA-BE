package com.example.jejugilmoa.domain.user.dto;

import com.example.jejugilmoa.domain.user.enums.TravelStyle;

public record TravelPreferenceResponse(
        Boolean nature,
        Boolean food,
        Boolean cafe,
        Boolean traditionMarket,
        Boolean history,
        Boolean experience,
        TravelStyle travelStyle
) {}
