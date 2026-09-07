package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.plan.dto.TravelPlanRouteJobClaim;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRouteJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class TravelPlanRouteJobService {
    public static final int LEASE_SECONDS = 120;
    public static final int HEARTBEAT_SECONDS = 30;
    private final TravelPlanRouteJobRepository jobs;

    // 독립 커밋을 금지하여 계획과 job이 반드시 함께 커밋/롤백되게 한다.
    @Transactional(propagation = Propagation.MANDATORY)
    public void enqueue(Long planId) {
        jobs.enqueue(planId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<TravelPlanRouteJobClaim> claim() {
        return jobs.claim(LEASE_SECONDS);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean renew(TravelPlanRouteJobClaim claim) {
        return jobs.renew(claim, LEASE_SECONDS);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void finish(TravelPlanRouteJobClaim claim, boolean success, String error) {
        jobs.finish(claim, success, retrySeconds(claim.attempt()), error);
    }

    static int retrySeconds(int attempt) {
        return (int) Math.min(900L, 30L << Math.min(Math.max(attempt - 1, 0), 5));
    }
}
