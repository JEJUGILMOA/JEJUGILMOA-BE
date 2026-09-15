package com.example.jejugilmoa.domain.report.exception;

import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ReportErrorCode implements BaseCode {

    REPORT_ALREADY_REPORTED(HttpStatus.CONFLICT, "REPORT409_1", "이미 신고한 게시글입니다."),
    REPORT_SELF_REPORT(HttpStatus.FORBIDDEN, "REPORT403_1", "본인이 작성한 게시글은 신고할 수 없습니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
