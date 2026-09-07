package com.example.jejugilmoa.global.scheduler;

import com.example.jejugilmoa.domain.plan.service.TravelPlanRouteWorker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.plan-route.worker", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TravelPlanRouteUpdateScheduler {
    private final TravelPlanRouteWorker worker;

    @Scheduled(fixedDelayString = "${app.plan-route.worker.poll-delay-ms:1000}",
            initialDelayString = "${app.plan-route.worker.initial-delay-ms:10000}")
    public void poll() {
        try {
            worker.runOnce();
        } catch (RuntimeException e) {
            // DB 장애도 다음 poll에서 재시도하며 RUNNING은 lease 만료 후 회수한다.
            log.error("경로 job poll 실패", e);
        }
    }
}
