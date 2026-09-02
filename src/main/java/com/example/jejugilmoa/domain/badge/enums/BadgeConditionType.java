package com.example.jejugilmoa.domain.badge.enums;

/**
 * 뱃지 지급 조건 유형.
 *
 * <p>방문 집계는 모두 실시간 GPS 방문 인증({@code TravelCourse.visited=true, skipped=false}) 기준이다.</p>
 */
public enum BadgeConditionType {
    PLACE,              // 특정 장소를 visitCount회 방문 (timeStart/timeEnd가 있으면 해당 시간대 인증만 집계)
    CATEGORY,           // 특정 카테고리에 속한 서로 다른 장소 visitCount곳 방문
    REGION,             // 주소에 지역명이 포함된 서로 다른 장소 visitCount곳 방문
    TOTAL_PLACES,       // 전체 서로 다른 장소 visitCount곳 방문
    CATEGORY_DIVERSITY, // 서로 다른 카테고리 visitCount개 방문
    TRIP_COUNT,         // 완료(COMPLETED)한 여행 visitCount회
    COURSE,             // 코스 경유지(badge_condition_course_stop)를 순서대로 전부 인증
    ALL_BADGES          // 자신을 제외한 모든 뱃지 보유 (마스터형)
}
