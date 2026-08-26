package com.example.jejugilmoa.domain.record.exception;

import com.example.jejugilmoa.global.apiPayload.code.BaseCode;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum RecordErrorCode implements BaseCode {
    RECORD_TRIP_ACCESS_DENIED(HttpStatus.FORBIDDEN, "RECORD403_1", "본인의 완료 여행만 기록할 수 있습니다."),
    RECORD_TRIP_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "RECORD400_1", "완료된 여행만 기록할 수 있습니다."),
    RECORD_MEMO_TARGET_MISMATCH(HttpStatus.BAD_REQUEST, "RECORD400_2", "메모 대상 경유지가 해당 여행에 포함되지 않습니다."),
    RECORD_INVALID_OBJECT_KEY(HttpStatus.BAD_REQUEST, "RECORD400_3", "유효하지 않은 이미지 objectKey입니다."),
    INVALID_PAGE(HttpStatus.BAD_REQUEST, "RECORD400_4", "페이지 번호는 0 이상이어야 합니다."),
    INVALID_PAGE_SIZE(HttpStatus.BAD_REQUEST, "RECORD400_5", "페이지 크기는 1 이상 100 이하여야 합니다."),
    RECORD_PLACE_TARGET_MISMATCH(HttpStatus.BAD_REQUEST, "RECORD400_6", "수정 대상 장소가 해당 여행 기록에 포함되지 않습니다."),
    RECORD_NOT_FOUND(HttpStatus.NOT_FOUND, "RECORD404_1", "여행 기록을 찾을 수 없습니다."),
    RECORD_ACCESS_DENIED(HttpStatus.FORBIDDEN, "RECORD403_2", "본인의 여행 기록만 수정하거나 삭제할 수 있습니다."),
    RECORD_ALREADY_DELETED(HttpStatus.GONE, "RECORD410_1", "이미 삭제된 여행 기록입니다."),
    RECORD_ALREADY_EXISTS(HttpStatus.CONFLICT, "RECORD409_1", "이미 해당 여행의 기록이 존재합니다.");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
