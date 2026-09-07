package com.example.jejugilmoa.domain.plan.service;

import jakarta.annotation.PreDestroy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Service
@RequiredArgsConstructor
public class TravelPlanRouteWorker {
    private final TravelPlanRouteJobService jobs;
    private final TravelPlanRouteService routes;
    // 기존 단일 scheduler가 외부 호출을 기다려도 heartbeat는 독립적으로 실행된다.
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(runnable -> {
        Thread thread = new Thread(runnable, "route-job-heartbeat");
        thread.setDaemon(true);
        return thread;
    });

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean runOnce() {
        var claimed = jobs.claim();
        if (claimed.isEmpty()) return false;
        var claim = claimed.get();
        AtomicBoolean lost = new AtomicBoolean();
        ScheduledFuture<?> renewal = heartbeat.scheduleAtFixedRate(() -> {
            try {
                if (!jobs.renew(claim)) lost.set(true);
            } catch (RuntimeException e) {
                lost.set(true);
                log.warn("경로 job lease 갱신 실패: jobId={}", claim.id(), e);
            }
        }, TravelPlanRouteJobService.HEARTBEAT_SECONDS, TravelPlanRouteJobService.HEARTBEAT_SECONDS, TimeUnit.SECONDS);
        try {
            boolean success = routes.refresh(claim, lost);
            if (!lost.get()) jobs.finish(claim, success, success ? null : "ROUTE_CALCULATION_FAILED");
        } catch (RuntimeException e) {
            log.error("경로 job 처리 실패: jobId={}", claim.id(), e);
            jobs.finish(claim, false, "ROUTE_JOB_FAILED");
        } finally {
            renewal.cancel(false);
        }
        return true;
    }

    @PreDestroy
    public void stopHeartbeat() {
        heartbeat.shutdownNow();
    }
}
