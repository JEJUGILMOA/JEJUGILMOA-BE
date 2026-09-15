package com.example.jejugilmoa.domain.record.enums;

public enum RecordAdminStatus {
    NORMAL,        // 정상
    UNDER_REVIEW,  // 신고 임계값 도달 → 관리자 심사 대기
    ADMIN_REMOVED  // 관리자 삭제 결정
}
