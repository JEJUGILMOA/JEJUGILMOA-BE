package com.example.jejugilmoa.domain.badge.dto;

import com.example.jejugilmoa.domain.badge.enums.BadgeGroup;

import java.util.List;

public record BadgeGroupResponse(
        BadgeGroup group,
        List<BadgeItemResponse> badges
) {}
