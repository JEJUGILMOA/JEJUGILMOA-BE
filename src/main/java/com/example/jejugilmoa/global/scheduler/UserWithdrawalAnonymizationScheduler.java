package com.example.jejugilmoa.global.scheduler;

import com.example.jejugilmoa.domain.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.user.withdrawal-anonymization",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class UserWithdrawalAnonymizationScheduler {

    private final UserService userService;
    private final Clock clock;

    @Scheduled(cron = "${app.user.withdrawal-anonymization.cron:0 0 4 * * *}", zone = "UTC")
    public void anonymizeExpiredWithdrawals() {
        LocalDateTime cutoff = LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC).minus(Period.ofDays(30));
        int anonymizedCount = userService.anonymizeExpiredWithdrawals(cutoff);
        log.info("탈퇴 30일 경과 계정 익명화 완료: anonymizedCount={}", anonymizedCount);
    }
}
