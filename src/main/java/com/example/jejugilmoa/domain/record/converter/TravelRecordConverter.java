package com.example.jejugilmoa.domain.record.converter;

import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCreateRequest;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCreateResponse;
import com.example.jejugilmoa.domain.record.entity.TravelRecord;
import com.example.jejugilmoa.domain.record.entity.TravelRecordImage;
import com.example.jejugilmoa.domain.record.entity.TravelRecordPlace;
import com.example.jejugilmoa.domain.user.entity.User;

public final class TravelRecordConverter {

    private TravelRecordConverter() {
    }

    public static TravelRecord toEntity(TravelRecordCreateRequest request, TravelPlan plan, User user) {
        return TravelRecord.builder()
                .travelPlan(plan)
                .user(user)
                .title(request.title())
                .description(request.description())
                .visibility(request.visibility() == null ? Visibility.PRIVATE : request.visibility())
                .actualStartDate(plan.getActualStartedAt() == null ? null : plan.getActualStartedAt().toLocalDate())
                .actualEndDate(plan.getActualCompletedAt() == null ? null : plan.getActualCompletedAt().toLocalDate())
                .build();
    }

    public static TravelRecordPlace toPlace(
            TravelRecord record, TravelCourse course, int sequenceOrder, String memo) {
        var place = course.getPlace();
        // TODO: 장소별 rating/stayMinutes 입력 정책 확정 후 validation 및 nullable 정책 재검토
        return TravelRecordPlace.builder()
                .travelRecord(record)
                .place(place)
                .placeName(place.getName())
                .address(place.getAddress())
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .visitDate(course.getVisitDate())
                .sequenceOrder(sequenceOrder)
                .visited(course.isVisited())
                .visitedAt(course.getVisitedAt())
                .memo(memo)
                .build();
    }

    public static TravelRecordImage toImage(TravelRecord record, String objectKey, int sequenceOrder) {
        return toImage(record, null, objectKey, sequenceOrder);
    }

    public static TravelRecordImage toImage(
            TravelRecord record, TravelRecordPlace recordPlace, String objectKey, int sequenceOrder) {
        // TODO: 조회 API 구현 시 sequenceOrder가 가장 빠른 이미지를 대표 이미지로 사용
        return TravelRecordImage.builder()
                .travelRecord(record)
                .travelRecordPlace(recordPlace)
                .objectKey(objectKey)
                .sequenceOrder(sequenceOrder)
                .build();
    }

    public static TravelRecordCreateResponse toCreateResponse(TravelRecord record, Long tripId) {
        return new TravelRecordCreateResponse(
                record.getId(), tripId, record.getTitle(), record.getVisibility(), record.getCreatedAt());
    }
}
