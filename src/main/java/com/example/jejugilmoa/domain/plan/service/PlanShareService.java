package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.plan.converter.PlanShareConverter;
import com.example.jejugilmoa.domain.plan.dto.ShareLinkResponse;
import com.example.jejugilmoa.domain.plan.dto.SharedPlanResponse;
import com.example.jejugilmoa.domain.plan.entity.DayDeparture;
import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.entity.TravelSharedPlan;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.repository.DayDepartureRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelCourseRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelSharedPlanRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PlanShareService {

    private static final Duration SHARE_VALIDITY = Duration.ofDays(30);
    private static final int TOKEN_GENERATION_ATTEMPTS = 3;

    private final TravelPlanRepository travelPlanRepository;
    private final TravelSharedPlanRepository travelSharedPlanRepository;
    private final TravelCourseRepository travelCourseRepository;
    private final DayDepartureRepository dayDepartureRepository;
    private final Clock clock;

    @Transactional
    public ShareLinkResponse createShareLink(Long planId, Long userId) {
        TravelPlan plan = travelPlanRepository.findByIdForUpdate(planId)
                .orElseThrow(() -> new GeneralException(PlanErrorCode.PLAN_NOT_FOUND));
        if (!plan.getUser().getId().equals(userId)) {
            throw new GeneralException(PlanErrorCode.PLAN_ACCESS_DENIED);
        }

        Instant now = clock.instant();
        TravelSharedPlan sharedPlan = travelSharedPlanRepository.findByTravelPlanId(planId).orElse(null);
        if (sharedPlan != null && sharedPlan.isAvailable(now)) {
            return PlanShareConverter.toShareLinkResponse(sharedPlan);
        }

        String token = generateUniqueToken();
        Instant expiresAt = now.plus(SHARE_VALIDITY);
        if (sharedPlan == null) {
            sharedPlan = TravelSharedPlan.issue(plan, token, expiresAt);
        } else {
            sharedPlan.reissue(token, expiresAt);
        }

        try {
            TravelSharedPlan saved = travelSharedPlanRepository.saveAndFlush(sharedPlan);
            return PlanShareConverter.toShareLinkResponse(saved);
        } catch (DataIntegrityViolationException exception) {
            throw new GeneralException(PlanErrorCode.SHARE_LINK_SAVE_FAILED);
        }
    }

    public SharedPlanResponse getSharedPlan(String shareToken) {
        TravelSharedPlan sharedPlan = travelSharedPlanRepository.findByShareToken(shareToken)
                .orElseThrow(() -> new GeneralException(PlanErrorCode.SHARE_TOKEN_NOT_FOUND));
        if (!sharedPlan.isActive()) {
            throw new GeneralException(PlanErrorCode.SHARE_LINK_INACTIVE);
        }
        if (sharedPlan.isExpired(clock.instant())) {
            throw new GeneralException(PlanErrorCode.SHARE_LINK_EXPIRED);
        }

        Long planId = sharedPlan.getTravelPlan().getId();
        TravelPlan plan = travelPlanRepository.findById(planId)
                .orElseThrow(() -> new GeneralException(PlanErrorCode.PLAN_NOT_FOUND));
        List<TravelCourse> courses = travelCourseRepository
                .findAllByTravelPlanIdOrderByVisitDateAscSequenceOrderAsc(planId);
        List<DayDeparture> dayDepartures = dayDepartureRepository.findAllByTravelPlanIdOrderByVisitDateAsc(planId);
        return PlanShareConverter.toSharedPlanResponse(plan, courses, dayDepartures);
    }

    private String generateUniqueToken() {
        for (int attempt = 0; attempt < TOKEN_GENERATION_ATTEMPTS; attempt++) {
            String token = UUID.randomUUID().toString();
            if (!travelSharedPlanRepository.existsByShareToken(token)) {
                return token;
            }
        }
        throw new GeneralException(PlanErrorCode.SHARE_LINK_SAVE_FAILED);
    }
}
