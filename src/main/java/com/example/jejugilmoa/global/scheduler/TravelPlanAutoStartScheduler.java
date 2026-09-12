package com.example.jejugilmoa.global.scheduler;

import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.domain.plan.service.TripService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class TravelPlanAutoStartScheduler {

    private final TravelPlanRepository travelPlanRepository;
    private final TripService tripService;

    // 매일 자정에 시작일이 오늘 이하인 DRAFT 계획을 자동으로 IN_PROGRESS로 전환
    @Scheduled(cron = "0 0 0 * * *")
    public void autoStartPlans() {
        LocalDate today = LocalDate.now();
        List<Long> planIds = travelPlanRepository.findDraftPlanIdsToAutoStart(today);

        if (planIds.isEmpty()) {
            return;
        }

        log.info("여행 자동 시작 대상: {}건 (기준일: {})", planIds.size(), today);

        for (Long planId : planIds) {
            try {
                tripService.autoStart(planId);
            } catch (Exception e) {
                log.error("여행 자동 시작 실패 (planId={})", planId, e);
            }
        }
    }
}
