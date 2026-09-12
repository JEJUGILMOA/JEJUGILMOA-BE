package com.example.jejugilmoa.domain.recommendation.service;

import com.example.jejugilmoa.domain.imageupload.service.ImageUrlResolver;
import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.record.entity.TravelRecord;
import com.example.jejugilmoa.domain.record.entity.TravelRecordImage;
import com.example.jejugilmoa.domain.record.entity.TravelRecordPlace;
import com.example.jejugilmoa.domain.record.repository.TravelRecordPlaceRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordRepository;
import com.example.jejugilmoa.domain.recommendation.dto.SaveCourseRequest;
import com.example.jejugilmoa.domain.recommendation.dto.SavedCourseDetailResponse;
import com.example.jejugilmoa.domain.recommendation.dto.SavedCourseListItemResponse;
import com.example.jejugilmoa.domain.recommendation.entity.RecommendedCourse;
import com.example.jejugilmoa.domain.recommendation.entity.SavedCourse;
import com.example.jejugilmoa.domain.recommendation.enums.CourseSourceType;
import com.example.jejugilmoa.domain.recommendation.exception.RecommendationErrorCode;
import com.example.jejugilmoa.domain.recommendation.repository.RecommendedCourseRepository;
import com.example.jejugilmoa.domain.recommendation.repository.SavedCourseRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SavedCourseService {

    private final SavedCourseRepository savedCourseRepository;
    private final RecommendedCourseRepository recommendedCourseRepository;
    private final TravelRecordRepository travelRecordRepository;
    private final TravelRecordPlaceRepository travelRecordPlaceRepository;
    private final UserRepository userRepository;
    private final ImageUrlResolver imageUrlResolver;

    @Transactional
    public void saveCourse(Long userId, SaveCourseRequest request) {
        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GeneralException(RecommendationErrorCode.COURSE_NOT_FOUND));

        if (request.sourceType() == CourseSourceType.RECOMMENDED) {
            saveRecommendedCourse(user, request.sourceId());
        } else {
            saveRecordCourse(user, userId, request.sourceId());
        }
    }

    private void saveRecommendedCourse(User user, Long courseId) {
        RecommendedCourse course = recommendedCourseRepository.findById(courseId)
                .orElseThrow(() -> new GeneralException(RecommendationErrorCode.COURSE_NOT_FOUND));

        if (savedCourseRepository.existsByUserIdAndRecommendedCourseId(user.getId(), courseId)) {
            throw new GeneralException(RecommendationErrorCode.COURSE_ALREADY_SAVED);
        }

        persistSavedCourse(SavedCourse.builder()
                .user(user)
                .sourceType(CourseSourceType.RECOMMENDED)
                .recommendedCourse(course)
                .build());

        recommendedCourseRepository.incrementCopyCount(courseId);
    }

    private void saveRecordCourse(User user, Long userId, Long recordId) {
        TravelRecord record = travelRecordRepository.findActiveByIdWithUserAndPlan(recordId)
                .orElseThrow(() -> new GeneralException(RecommendationErrorCode.COURSE_NOT_FOUND));

        boolean isOwner = record.getUser().getId().equals(userId);
        boolean isPublic = record.getVisibility() == Visibility.PUBLIC;
        if (!isOwner && !isPublic) {
            throw new GeneralException(RecommendationErrorCode.RECORD_COURSE_NOT_ACCESSIBLE);
        }

        if (savedCourseRepository.existsByUserIdAndTravelRecordId(userId, recordId)) {
            throw new GeneralException(RecommendationErrorCode.COURSE_ALREADY_SAVED);
        }

        persistSavedCourse(SavedCourse.builder()
                .user(user)
                .sourceType(CourseSourceType.RECORD)
                .travelRecord(record)
                .build());
    }

    // uq_saved_course_recommended / uq_saved_course_record 충돌 → COURSE_ALREADY_SAVED(409)
    private void persistSavedCourse(SavedCourse savedCourse) {
        try {
            savedCourseRepository.saveAndFlush(savedCourse);
        } catch (DataIntegrityViolationException e) {
            String msg = e.getMostSpecificCause().getMessage();
            if (msg != null && (msg.contains("uq_saved_course_recommended") || msg.contains("uq_saved_course_record"))) {
                throw new GeneralException(RecommendationErrorCode.COURSE_ALREADY_SAVED);
            }
            throw e;
        }
    }

    @Transactional
    public void removeSavedCourse(Long userId, Long savedCourseId) {
        SavedCourse savedCourse = savedCourseRepository.findById(savedCourseId)
                .orElseThrow(() -> new GeneralException(RecommendationErrorCode.SAVED_COURSE_NOT_FOUND));

        if (!savedCourse.getUser().getId().equals(userId)) {
            throw new GeneralException(RecommendationErrorCode.RECORD_COURSE_NOT_ACCESSIBLE);
        }

        if (savedCourse.getSourceType() == CourseSourceType.RECOMMENDED) {
            recommendedCourseRepository.decrementCopyCount(savedCourse.getRecommendedCourse().getId());
        }

        savedCourseRepository.delete(savedCourse);
    }

    public List<SavedCourseListItemResponse> getSavedCourses(Long userId) {
        List<SavedCourse> savedCourses = savedCourseRepository.findAllByUserIdWithSources(userId);
        if (savedCourses.isEmpty()) {
            return List.of();
        }

        List<Long> recommendedIds = savedCourses.stream()
                .filter(sc -> sc.getSourceType() == CourseSourceType.RECOMMENDED)
                .map(sc -> sc.getRecommendedCourse().getId())
                .toList();

        List<Long> recordIds = savedCourses.stream()
                .filter(sc -> sc.getSourceType() == CourseSourceType.RECORD)
                .map(sc -> sc.getTravelRecord().getId())
                .toList();

        Map<Long, Integer> recommendedPlaceCounts = batchCountPaths(recommendedIds);
        Map<Long, Integer> recordPlaceCounts = batchCountRecordPlaces(recordIds);

        return savedCourses.stream()
                .map(sc -> toListItem(sc, recommendedPlaceCounts, recordPlaceCounts))
                .toList();
    }

    private Map<Long, Integer> batchCountPaths(Collection<Long> courseIds) {
        if (courseIds.isEmpty()) return Map.of();
        return recommendedCourseRepository.countPathsByCourseIds(courseIds).stream()
                .collect(Collectors.toMap(
                        RecommendedCourseRepository.CoursePathCount::getCourseId,
                        c -> c.getCount().intValue()
                ));
    }

    private Map<Long, Integer> batchCountRecordPlaces(Collection<Long> recordIds) {
        if (recordIds.isEmpty()) return Map.of();
        return travelRecordPlaceRepository.countAllByRecordIds(recordIds).stream()
                .collect(Collectors.toMap(
                        TravelRecordPlaceRepository.RecordPlaceCount::getRecordId,
                        c -> c.getCount().intValue()
                ));
    }

    public SavedCourseDetailResponse getSavedCourseDetail(Long userId, Long savedCourseId) {
        SavedCourse savedCourse = savedCourseRepository.findById(savedCourseId)
                .orElseThrow(() -> new GeneralException(RecommendationErrorCode.SAVED_COURSE_NOT_FOUND));

        if (!savedCourse.getUser().getId().equals(userId)) {
            throw new GeneralException(RecommendationErrorCode.RECORD_COURSE_NOT_ACCESSIBLE);
        }

        if (savedCourse.getSourceType() == CourseSourceType.RECOMMENDED) {
            return toDetailFromRecommended(savedCourse);
        } else {
            return toDetailFromRecord(savedCourse);
        }
    }

    private SavedCourseListItemResponse toListItem(SavedCourse sc,
                                                    Map<Long, Integer> recommendedPlaceCounts,
                                                    Map<Long, Integer> recordPlaceCounts) {
        if (sc.getSourceType() == CourseSourceType.RECOMMENDED) {
            RecommendedCourse rc = sc.getRecommendedCourse();
            int placeCount = recommendedPlaceCounts.getOrDefault(rc.getId(), 0);
            return new SavedCourseListItemResponse(
                    sc.getId(),
                    CourseSourceType.RECOMMENDED,
                    rc.getTitle(),
                    rc.getImageUrl(),
                    rc.getRegion(),
                    placeCount,
                    rc.getEstimatedMinutes()
            );
        } else {
            TravelRecord record = sc.getTravelRecord();
            TravelRecordImage thumbnail = record.getThumbnailImage();
            String imageUrl = thumbnail != null
                    ? imageUrlResolver.resolve(thumbnail.getObjectKey()) : null;
            int placeCount = recordPlaceCounts.getOrDefault(record.getId(), 0);
            return new SavedCourseListItemResponse(
                    sc.getId(),
                    CourseSourceType.RECORD,
                    record.getTitle(),
                    imageUrl,
                    null,
                    placeCount,
                    null
            );
        }
    }

    private SavedCourseDetailResponse toDetailFromRecommended(SavedCourse sc) {
        RecommendedCourse rc = recommendedCourseRepository.findByIdWithPaths(sc.getRecommendedCourse().getId())
                .orElseThrow(() -> new GeneralException(RecommendationErrorCode.COURSE_NOT_FOUND));

        List<SavedCourseDetailResponse.CourseStopItem> stops = rc.getPaths().stream()
                .map(path -> new SavedCourseDetailResponse.CourseStopItem(
                        path.getSequenceOrder(),
                        path.getPlace().getId(),
                        path.getPlace().getName(),
                        path.getPlace().getImageUrl(),
                        path.getTravelTimeToNext()
                ))
                .toList();

        List<String> tags = rc.getTags() != null && !rc.getTags().isBlank()
                ? Arrays.asList(rc.getTags().split(",")) : List.of();

        return new SavedCourseDetailResponse(
                sc.getId(),
                CourseSourceType.RECOMMENDED,
                rc.getTitle(),
                rc.getImageUrl(),
                rc.getRegion(),
                rc.getTheme(),
                tags,
                stops.size(),
                rc.getEstimatedMinutes(),
                rc.getDescription(),
                stops
        );
    }

    private SavedCourseDetailResponse toDetailFromRecord(SavedCourse sc) {
        TravelRecord record = sc.getTravelRecord();
        List<TravelRecordPlace> places =
                travelRecordPlaceRepository.findAllByRecordIdInSnapshotOrder(record.getId());

        TravelRecordImage detailThumbnail = record.getThumbnailImage();
        String imageUrl = detailThumbnail != null
                ? imageUrlResolver.resolve(detailThumbnail.getObjectKey()) : null;

        List<SavedCourseDetailResponse.CourseStopItem> stops = places.stream()
                .map(rp -> new SavedCourseDetailResponse.CourseStopItem(
                        rp.getSequenceOrder(),
                        rp.getPlace().getId(),
                        rp.getPlaceName(),
                        rp.getPlace().getImageUrl(),
                        null
                ))
                .toList();

        return new SavedCourseDetailResponse(
                sc.getId(),
                CourseSourceType.RECORD,
                record.getTitle(),
                imageUrl,
                null,
                null,
                null,
                stops.size(),
                null,
                record.getDescription(),
                stops
        );
    }
}
