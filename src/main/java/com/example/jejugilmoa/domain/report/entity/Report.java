package com.example.jejugilmoa.domain.report.entity;


import com.example.jejugilmoa.domain.report.enums.ReportStatus;
import com.example.jejugilmoa.domain.report.enums.TargetType;
import com.example.jejugilmoa.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "report",
        indexes = {
                @Index(name = "idx_report_target", columnList = "target_type,target_id"),
                @Index(name = "idx_report_status", columnList = "status"),
                @Index(name = "idx_report_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Report extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "reporter_id")
    private Long reporterId;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private TargetType targetType; //RECORD,PHOTO,USER


    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @Column(length = 200)
    private String reason;


    // PENDING,APPROVED,REJECTED,RESOLVED
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private ReportStatus status = ReportStatus.PENDING;

    // 처리 시간
    private LocalDateTime processedAt;

    // ====== 상태 변경 메서드 (핵심) ======

    public void approve() {
        this.status = ReportStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
    }

    public void reject() {
        this.status = ReportStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
    }

    public void resolve() {
        this.status = ReportStatus.RESOLVED;
        this.processedAt = LocalDateTime.now();
    }
}
