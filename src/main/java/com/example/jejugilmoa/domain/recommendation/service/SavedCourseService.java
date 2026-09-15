package com.example.jejugilmoa.domain.recommendation.service;

import com.example.jejugilmoa.domain.imageupload.service.ImageUrlResolver;
import com.example.jejugilmoa.domain.plan.enums.Visibility;
import com.example.jejugilmoa.domain.record.entity.TravelRecord;
import com.example.jejugilmoa.domain.record.entity.TravelRecordImage;
import com.example.jejugilmoa.domain.record.entity.TravelRecordPlace;
import com.example.jejugilmoa.domain.record.repository.TravelRecordPlaceRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordRepository;
import com.example.jejugilmoa.domain.recommendation.dto.CourseWaypointItem;
import com.example.jejugilmoa.domain.recommendation.entity.RecommendedCoursePath;
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
import java.util.Comparator;
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

        Map<Long, List<CourseWaypointItem>> recommendedWaypoints = batchRecommendedWaypoints(recommendedIds);
        Map<Long, List<CourseWaypointItem>> recordWaypoints = batchRecordWaypoints(recordIds);

        return savedCourses.stream()
                .map(sc -> toListItem(sc, recommendedWaypoints, recordWaypoints))
                .toList();
    }

    private Map<Long, List<CourseWaypointItem>> batchRecommendedWaypoints(Collection<Long> courseIds) {
        if (courseIds.isEmpty()) return Map.of();
        return recommendedCourseRepository.findAllByIdInWithPaths(courseIds).stream()
                .collect(Collectors.toMap(RecommendedCourse::getId, course -> course.getPaths().stream()
                        .sorted(Comparator.comparing(RecommendedCoursePath::getSequenceOrder))
                        .map(path -> new CourseWaypointItem(
                                path.getSequenceOrder(), path.getPlace().getId(),
                                path.getPlace().getName(), path.getPlace().getImageUrl(),
                                path.getPlace().getLatitude(), path.getPlace().getLongitude()))
                        .toList()));
    }

    private Map<Long, List<CourseWaypointItem>> batchRecordWaypoints(Collection<Long> recordIds) {
        if (recordIds.isEmpty()) return Map.of();
        return travelRecordPlaceRepository.findAllByRecordIdsWithPlaceInSnapshotOrder(recordIds).stream()
                .sorted(Comparator.comparing(TravelRecordPlace::getVisitDate)
                        .thenComparingInt(TravelRecordPlace::getSequenceOrder))
                .collect(Collectors.groupingBy(rp -> rp.getTravelRecord().getId(),
                        Collectors.mapping(rp -> new CourseWaypointItem(
                                rp.getSequenceOrder(), rp.getPlace().getId(), rp.getPlaceName(),
                                rp.getPlace().getImageUrl(), rp.getLatitude(), rp.getLongitude()),
                                Collectors.toList())));
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
                                                    Map<Long, List<CourseWaypointItem>> recommendedWaypoints,
                                                    Map<Long, List<CourseWaypointItem>> recordWaypoints) {
        if (sc.getSourceType() == CourseSourceType.RECOMMENDED) {
            RecommendedCourse rc = sc.getRecommendedCourse();
            List<CourseWaypointItem> waypoints = recommendedWaypoints.getOrDefault(rc.getId(), List.of());
            return new SavedCourseListItemResponse(
                    sc.getId(),
                    CourseSourceType.RECOMMENDED,
                    rc.getTitle(),
                    rc.getImageUrl(),
                    rc.getRegion(),
                    waypoints.size(),
                    rc.getEstimatedMinutes(),
                    rc.getId(), rc.getTheme(), rc.getDescription(), rc.getCopyCount(), waypoints
            );
        } else {
            TravelRecord record = sc.getTravelRecord();
            TravelRecordImage thumbnail = record.getThumbnailImage();
            String imageUrl = thumbnail != null
                    ? imageUrlResolver.resolve(thumbnail.getObjectKey()) : null;
            List<CourseWaypointItem> waypoints = recordWaypoints.getOrDefault(record.getId(), List.of());
            return new SavedCourseListItemResponse(
                    sc.getId(),
                    CourseSourceType.RECORD,
                    record.getTitle(),
                    imageUrl,
                    null,
                    waypoints.size(),
                    null,
                    record.getId(), null, record.getDescription(), null, waypoints
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
