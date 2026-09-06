package com.example.jejugilmoa.domain.record.converter;

import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCreateRequest;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCreateResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordAuthorResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCardResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordDetailResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordImageResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordMapPlaceResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordMapResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordPlaceResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordPlanLinkResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordUpdateResponse;
import com.example.jejugilmoa.domain.record.entity.TravelRecord;
import com.example.jejugilmoa.domain.record.entity.TravelRecordImage;
import com.example.jejugilmoa.domain.record.entity.TravelRecordPlace;
import com.example.jejugilmoa.domain.record.enums.ReactionType;
import com.example.jejugilmoa.domain.user.entity.User;

import java.util.List;

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

    public static TravelRecordUpdateResponse toUpdateResponse(TravelRecord record) {
        return new TravelRecordUpdateResponse(
                record.getId(), record.getTitle(), record.getDescription(), record.getVisibility(),
                record.getUpdatedAt());
    }

    public static TravelRecordAuthorResponse toAuthorResponse(User user) {
        return new TravelRecordAuthorResponse(user.getId(), user.getNickname(), user.getProfileImageUrl());
    }

    public static TravelRecordCardResponse toCardResponse(
            TravelRecord record,
            String thumbnailUrl,
            long visitedPlaceCount,
            long photoCount,
            long likeCount,
            long dislikeCount,
            ReactionType myReaction) {
        return new TravelRecordCardResponse(
                record.getId(), record.getTitle(), record.getDescription(), record.getVisibility(),
                thumbnailUrl, visitedPlaceCount, photoCount, likeCount, dislikeCount, myReaction,
                record.getActualStartDate(), record.getActualEndDate(), record.getCreatedAt(),
                toAuthorResponse(record.getUser()));
    }

    public static TravelRecordMapPlaceResponse toMapPlaceResponse(TravelRecordPlace place) {
        return new TravelRecordMapPlaceResponse(
                place.getId(), place.getPlace().getId(), place.getPlaceName(),
                place.getLatitude(), place.getLongitude(), place.getVisitDate(),
                place.getSequenceOrder(), place.isVisited());
    }

    public static TravelRecordMapResponse toMapResponse(
            TravelRecord record, List<TravelRecordMapPlaceResponse> places) {
        return new TravelRecordMapResponse(
                record.getId(), record.getTitle(), record.getVisibility(), record.getCreatedAt(),
                toAuthorResponse(record.getUser()), places);
    }

    public static TravelRecordImageResponse toImageResponse(TravelRecordImage image, String imageUrl) {
        return new TravelRecordImageResponse(image.getId(), imageUrl, image.getObjectKey(), image.getSequenceOrder());
    }

    public static TravelRecordPlaceResponse toPlaceResponse(
            TravelRecordPlace place, List<TravelRecordImageResponse> images) {
        return new TravelRecordPlaceResponse(
                place.getId(), place.getPlace().getId(), place.getPlaceName(), place.getAddress(),
                place.getLatitude(), place.getLongitude(), place.getVisitDate(), place.getSequenceOrder(),
                place.isVisited(), place.getVisitedAt(), place.getMemo(), place.getStayMinutes(),
                place.getRating(), images);
    }

    public static TravelRecordDetailResponse toDetailResponse(
            TravelRecord record,
            List<TravelRecordImageResponse> recordImages,
            List<TravelRecordImageResponse> allImages,
            List<TravelRecordPlaceResponse> places,
            long likeCount,
            long dislikeCount,
            ReactionType myReaction) {
        TravelRecordPlanLinkResponse plan = record.getTravelPlan() == null
                ? null
                : new TravelRecordPlanLinkResponse(
                        record.getTravelPlan().getId(), record.getTravelPlan().getTitle());
        return new TravelRecordDetailResponse(
                record.getId(), record.getTitle(), record.getDescription(), record.getVisibility(),
                record.getActualStartDate(), record.getActualEndDate(), record.getCreatedAt(),
                record.getUpdatedAt(),
                record.getThumbnailImage() == null ? null : record.getThumbnailImage().getId(),
                record.getThumbnailImage() == null ? null : imageUrl(record.getThumbnailImage(), allImages),
                toAuthorResponse(record.getUser()), plan, recordImages,
                allImages.size(), allImages, places,
                likeCount, dislikeCount, myReaction);
    }

    private static String imageUrl(
            TravelRecordImage thumbnailImage, List<TravelRecordImageResponse> allImages) {
        return allImages.stream()
                .filter(image -> image.imageId().equals(thumbnailImage.getId()))
                .map(TravelRecordImageResponse::imageUrl)
                .findFirst()
                .orElse(null);
    }
}
