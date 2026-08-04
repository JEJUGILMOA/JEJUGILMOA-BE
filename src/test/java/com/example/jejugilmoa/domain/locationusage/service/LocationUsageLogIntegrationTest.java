package com.example.jejugilmoa.domain.locationusage.service;

import com.example.jejugilmoa.domain.locationusage.entity.LocationUsageLog;
import com.example.jejugilmoa.domain.locationusage.enums.LocationAcquisitionPath;
import com.example.jejugilmoa.domain.locationusage.enums.LocationServiceCode;
import com.example.jejugilmoa.domain.locationusage.repository.LocationUsageLogRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest(properties = "app.location-usage.retention.enabled=false")
class LocationUsageLogIntegrationTest {

    @Autowired LocationUsageLogService locationUsageLogService;
    @Autowired LocationUsageLogRepository locationUsageLogRepository;
    @Autowired TransactionTemplate transactionTemplate;

    @AfterEach
    void cleanUp() {
        locationUsageLogRepository.deleteAll();
    }

    @Test
    void requiresNew_recordRemainsAfterOuterTransactionRollback() {
        assertThatThrownBy(() -> transactionTemplate.executeWithoutResult(status -> {
            locationUsageLogService.recordVisitVerification(77L);
            throw new RollbackTestException();
        })).isInstanceOf(RollbackTestException.class);

        assertThat(locationUsageLogRepository.findAll())
                .singleElement()
                .satisfies(log -> assertThat(log.getSubjectId()).isEqualTo(77L));
    }

    @Test
    void retention_deletesOnlyRowsStrictlyOlderThanOneYearCutoff() {
        Instant cutoff = Instant.parse("2025-08-02T00:00:00Z");
        // PostgreSQL TIMESTAMPTZ(6)는 마이크로초 정밀도이므로 DB에서 구분 가능한 경계값을 사용한다.
        locationUsageLogRepository.saveAndFlush(logAt(cutoff.minusSeconds(1)));
        locationUsageLogRepository.saveAndFlush(logAt(cutoff));
        locationUsageLogRepository.saveAndFlush(logAt(cutoff.plusSeconds(1)));

        long deleted = locationUsageLogService.deleteExpired(cutoff);

        assertThat(deleted).isEqualTo(1L);
        assertThat(locationUsageLogRepository.findAll())
                .extracting(LocationUsageLog::getReceivedAt)
                .containsExactlyInAnyOrder(cutoff, cutoff.plusSeconds(1));
    }

    private LocationUsageLog logAt(Instant receivedAt) {
        return LocationUsageLog.builder()
                .subjectId(88L)
                .acquisitionPath(LocationAcquisitionPath.CLIENT_DEVICE_LOCATION)
                .serviceCode(LocationServiceCode.VISIT_VERIFICATION)
                .receivedAt(receivedAt)
                .build();
    }

    private static class RollbackTestException extends RuntimeException {
    }
}
