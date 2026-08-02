package com.example.jejugilmoa.domain.locationusage.service;

import com.example.jejugilmoa.domain.locationusage.entity.LocationUsageLog;
import com.example.jejugilmoa.domain.locationusage.enums.LocationAcquisitionPath;
import com.example.jejugilmoa.domain.locationusage.enums.LocationServiceCode;
import com.example.jejugilmoa.domain.locationusage.repository.LocationUsageLogRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class LocationUsageLogServiceTest {

    private static final Instant RECEIVED_AT = Instant.parse("2026-08-02T12:34:56.789Z");

    @Mock LocationUsageLogRepository locationUsageLogRepository;
    @Spy Clock clock = Clock.fixed(RECEIVED_AT, ZoneOffset.UTC);
    @InjectMocks LocationUsageLogService locationUsageLogService;

    @Test
    void recordVisitVerification_savesFixedPolicyWithoutCoordinates() {
        given(locationUsageLogRepository.saveAndFlush(any(LocationUsageLog.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        locationUsageLogService.recordVisitVerification(42L);

        var captor = ArgumentCaptor.forClass(LocationUsageLog.class);
        verify(locationUsageLogRepository).saveAndFlush(captor.capture());
        LocationUsageLog saved = captor.getValue();
        assertThat(saved.getSubjectId()).isEqualTo(42L);
        assertThat(saved.getAcquisitionPath()).isEqualTo(LocationAcquisitionPath.CLIENT_DEVICE_LOCATION);
        assertThat(saved.getServiceCode()).isEqualTo(LocationServiceCode.VISIT_VERIFICATION);
        assertThat(saved.getRecipient()).isNull();
        assertThat(saved.getReceivedAt()).isEqualTo(RECEIVED_AT);
    }

    @Test
    void entity_hasNoRawCoordinateFields() {
        assertThat(LocationUsageLog.class.getDeclaredFields())
                .extracting(java.lang.reflect.Field::getName)
                .doesNotContain("latitude", "longitude");
    }

    @Test
    void deleteExpired_usesStrictBeforeCutoff() {
        Instant cutoff = Instant.parse("2025-08-02T00:00:00Z");
        given(locationUsageLogRepository.deleteByReceivedAtBefore(cutoff)).willReturn(3L);

        assertThat(locationUsageLogService.deleteExpired(cutoff)).isEqualTo(3L);

        verify(locationUsageLogRepository).deleteByReceivedAtBefore(cutoff);
    }
}
