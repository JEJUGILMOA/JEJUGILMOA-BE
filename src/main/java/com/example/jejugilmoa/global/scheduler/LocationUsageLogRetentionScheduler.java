package com.example.jejugilmoa.global.scheduler;

import com.example.jejugilmoa.domain.locationusage.service.LocationUsageLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Period;
import java.time.ZoneOffset;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "app.location-usage.retention",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
public class LocationUsageLogRetentionScheduler {

    private final LocationUsageLogService locationUsageLogService;
    private final Clock clock;

    @Scheduled(cron = "${app.location-usage.retention.cron:0 30 3 * * *}", zone = "UTC")
    public void deleteExpiredLogs() {
        var cutoff = clock.instant().atZone(ZoneOffset.UTC).minus(Period.ofYears(1)).toInstant();
        long deletedCount = locationUsageLogService.deleteExpired(cutoff);
        log.info("위치정보 이용기록 보존기간 만료 삭제 완료: deletedCount={}", deletedCount);
    }
}
