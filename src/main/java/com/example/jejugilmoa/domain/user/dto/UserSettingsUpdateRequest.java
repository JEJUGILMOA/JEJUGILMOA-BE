package com.example.jejugilmoa.domain.user.dto;

public record UserSettingsUpdateRequest(
        Boolean notifyPlanStart,
        Boolean notifyRecordWriting,
        Boolean notifyBadgeAcquired,
        Boolean notifyNextPlace,
        Boolean notifyPlaceArrival,
        Boolean notifyMarketing,
        Boolean locationPermission
) {}
