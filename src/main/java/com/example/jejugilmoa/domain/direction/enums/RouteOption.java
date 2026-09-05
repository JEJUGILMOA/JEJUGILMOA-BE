package com.example.jejugilmoa.domain.direction.enums;

import com.example.jejugilmoa.domain.direction.exception.DirectionErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/** 네이버 Directions 5 경로 탐색 옵션. */
@Getter
@AllArgsConstructor
public enum RouteOption {
    TRAFAST("trafast"),           // 실시간 빠른 길
    TRACOMFORT("tracomfort"),     // 실시간 편한 길
    TRAOPTIMAL("traoptimal"),     // 실시간 최적 (기본값)
    TRAAVOIDTOLL("traavoidtoll"), // 무료 우선
    TRAAVOIDCARONLY("traavoidcaronly"); // 자동차 전용 도로 회피

    private final String value;

    public static RouteOption from(String value) {
        for (RouteOption option : values()) {
            if (option.value.equalsIgnoreCase(value)) {
                return option;
            }
        }
        throw new GeneralException(DirectionErrorCode.INVALID_OPTION);
    }
}
