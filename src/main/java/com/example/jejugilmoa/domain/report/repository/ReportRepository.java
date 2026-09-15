package com.example.jejugilmoa.domain.report.repository;

import com.example.jejugilmoa.domain.report.entity.Report;
import com.example.jejugilmoa.domain.report.enums.ReportStatus;
import com.example.jejugilmoa.domain.report.enums.TargetType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByReporterIdAndTargetTypeAndTargetId(Long reporterId, TargetType targetType, Long targetId);

    long countByTargetTypeAndTargetIdAndStatus(TargetType targetType, Long targetId, ReportStatus status);
}
