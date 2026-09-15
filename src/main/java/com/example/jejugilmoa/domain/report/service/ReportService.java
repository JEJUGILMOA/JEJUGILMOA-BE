package com.example.jejugilmoa.domain.report.service;

import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.record.entity.TravelRecord;
import com.example.jejugilmoa.domain.record.exception.RecordErrorCode;
import com.example.jejugilmoa.domain.record.repository.TravelRecordRepository;
import com.example.jejugilmoa.domain.report.dto.ReportCreateRequest;
import com.example.jejugilmoa.domain.report.entity.Report;
import com.example.jejugilmoa.domain.report.enums.ReportStatus;
import com.example.jejugilmoa.domain.report.enums.TargetType;
import com.example.jejugilmoa.domain.report.exception.ReportErrorCode;
import com.example.jejugilmoa.domain.report.repository.ReportRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final int REPORT_HIDE_THRESHOLD = 5;

    private final ReportRepository reportRepository;
    private final TravelRecordRepository travelRecordRepository;

    @Transactional
    public void reportRecord(Long recordId, Long reporterId, ReportCreateRequest request) {
        TravelRecord record = travelRecordRepository.findActiveByIdWithUserAndPlan(recordId)
                .orElseThrow(() -> new GeneralException(RecordErrorCode.RECORD_NOT_FOUND));

        // 비공개 기록은 접근 불가 — 신고자 입장에서 존재하지 않는 것으로 처리
        if (record.getVisibility() != Visibility.PUBLIC) {
            throw new GeneralException(RecordErrorCode.RECORD_NOT_FOUND);
        }

        if (record.getUser().getId().equals(reporterId)) {
            throw new GeneralException(ReportErrorCode.REPORT_SELF_REPORT);
        }

        if (reportRepository.existsByReporterIdAndTargetTypeAndTargetId(
                reporterId, TargetType.RECORD, recordId)) {
            throw new GeneralException(ReportErrorCode.REPORT_ALREADY_REPORTED);
        }

        reportRepository.save(Report.builder()
                .reporterId(reporterId)
                .targetType(TargetType.RECORD)
                .targetId(recordId)
                .reasonSummary(request.reasonSummary())
                .reasonDetail(request.reasonDetail())
                .build());

        long pendingCount = reportRepository.countByTargetTypeAndTargetIdAndStatus(
                TargetType.RECORD, recordId, ReportStatus.PENDING);
        if (pendingCount >= REPORT_HIDE_THRESHOLD) {
            record.hideByReport();
        }
    }
}
