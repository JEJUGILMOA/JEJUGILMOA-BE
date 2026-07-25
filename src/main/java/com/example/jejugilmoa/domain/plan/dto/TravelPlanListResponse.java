package com.example.jejugilmoa.domain.plan.dto;

import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;

import java.time.LocalDate;

public record TravelPlanListResponse(
        Long planId,
        String title,
        LocalDate startDate,
        LocalDate endDate,
        TravelPlanStatus status,
        int waypointCount,
        int nights,   // 박 수 (0이면 당일치기)
        int days,     // 일 수 (nights + 1)
        int dDay      // 출발까지 남은 일수 (양수=미래, 0=오늘, 음수=과거)
) {}
