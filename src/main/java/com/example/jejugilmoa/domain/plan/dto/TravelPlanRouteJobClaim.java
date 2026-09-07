package com.example.jejugilmoa.domain.plan.dto;

import java.util.UUID;

/** worker 내부 소유권. API 응답에 노출하지 않는다. */
public record TravelPlanRouteJobClaim(Long id, Long planId, UUID token, int attempt) {}
