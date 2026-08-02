package com.example.jejugilmoa.domain.locationusage.entity;

import com.example.jejugilmoa.domain.locationusage.enums.LocationAcquisitionPath;
import com.example.jejugilmoa.domain.locationusage.enums.LocationServiceCode;
import com.example.jejugilmoa.global.entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "location_usage_log", indexes = {
        @Index(name = "idx_location_usage_log_received_at", columnList = "received_at")
})
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LocationUsageLog extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 사용자 탈퇴 후에도 법정 확인자료를 보존하도록 User 연관관계와 FK를 두지 않는다.
    @Column(name = "subject_id", nullable = false)
    private Long subjectId;

    @Enumerated(EnumType.STRING)
    @Column(name = "acquisition_path", nullable = false, length = 50)
    private LocationAcquisitionPath acquisitionPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "service_code", nullable = false, length = 50)
    private LocationServiceCode serviceCode;

    @Column(length = 255)
    private String recipient;

    @Column(name = "received_at", nullable = false)
    private Instant receivedAt;
}
