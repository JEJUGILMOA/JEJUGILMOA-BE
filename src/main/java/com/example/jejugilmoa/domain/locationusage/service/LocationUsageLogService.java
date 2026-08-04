package com.example.jejugilmoa.domain.locationusage.service;

import com.example.jejugilmoa.domain.locationusage.entity.LocationUsageLog;
import com.example.jejugilmoa.domain.locationusage.enums.LocationAcquisitionPath;
import com.example.jejugilmoa.domain.locationusage.enums.LocationServiceCode;
import com.example.jejugilmoa.domain.locationusage.repository.LocationUsageLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LocationUsageLogService {

    private static final LocationAcquisitionPath ACQUISITION_PATH =
            LocationAcquisitionPath.CLIENT_DEVICE_LOCATION;
    private static final LocationServiceCode SERVICE_CODE =
            LocationServiceCode.VISIT_VERIFICATION;

    private final LocationUsageLogRepository locationUsageLogRepository;
    private final Clock clock;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Long recordVisitVerification(Long subjectId) {
        LocationUsageLog usageLog = LocationUsageLog.builder()
                .subjectId(subjectId)
                .acquisitionPath(ACQUISITION_PATH)
                .serviceCode(SERVICE_CODE)
                .recipient(null)
                .receivedAt(clock.instant())
                .build();

        // 비즈니스 검증 전에 DB 반영 실패를 확정해 방문 인증 진행을 차단한다.
        return locationUsageLogRepository.saveAndFlush(usageLog).getId();
    }

    @Transactional
    public long deleteExpired(Instant cutoff) {
        return locationUsageLogRepository.deleteByReceivedAtBefore(cutoff);
    }
}
