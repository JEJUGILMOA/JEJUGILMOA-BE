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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

        savedCourseRepository.save(SavedCourse.builder()
                .user(user)
                .sourceType(CourseSourceType.RECOMMENDED)
                .recommendedCourse(course)
                .build());

        course.incrementCopyCount();
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

        savedCourseRepository.save(SavedCourse.builder()
                .user(user)
                .sourceType(CourseSourceType.RECORD)
                .travelRecord(record)
                .build());
    }

    @Transactional
    public void removeSavedCourse(Long userId, Long savedCourseId) {
        SavedCourse savedCourse = savedCourseRepository.findById(savedCourseId)
                .orElseThrow(() -> new GeneralException(RecommendationErrorCode.SAVED_COURSE_NOT_FOUND));

        if (!savedCourse.getUser().getId().equals(userId)) {
            throw new GeneralException(RecommendationErrorCode.RECORD_COURSE_NOT_ACCESSIBLE);
        }

        if (savedCourse.getSourceType() == CourseSourceType.RECOMMENDED) {
            savedCourse.getRecommendedCourse().decrementCopyCount();
        }

        savedCourseRepository.delete(savedCourse);
    }

    public List<SavedCourseListItemResponse> getSavedCourses(Long userId) {
        return savedCourseRepository.findAllByUserIdWithSources(userId).stream()
                .map(this::toListItem)
                .toList();
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

    private SavedCourseListItemResponse toListItem(SavedCourse sc) {
        if (sc.getSourceType() == CourseSourceType.RECOMMENDED) {
            RecommendedCourse rc = sc.getRecommendedCourse();
            return new SavedCourseListItemResponse(
                    sc.getId(),
                    CourseSourceType.RECOMMENDED,
                    rc.getTitle(),
                    rc.getImageUrl(),
                    rc.getRegion(),
                    rc.getPaths().size(),
                    rc.getEstimatedMinutes(),
                    rc.getTransportMode()
            );
        } else {
            TravelRecord record = sc.getTravelRecord();
            TravelRecordImage thumbnail = record.getThumbnailImage();
            String imageUrl = thumbnail != null
                    ? imageUrlResolver.resolve(thumbnail.getObjectKey()) : null;
            List<TravelRecordPlace> places =
                    travelRecordPlaceRepository.findAllByRecordIdInSnapshotOrder(record.getId());
            return new SavedCourseListItemResponse(
                    sc.getId(),
                    CourseSourceType.RECORD,
                    record.getTitle(),
                    imageUrl,
                    null,
                    places.size(),
                    null,
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

        return new SavedCourseDetailResponse(
                sc.getId(),
                CourseSourceType.RECOMMENDED,
                rc.getTitle(),
                rc.getImageUrl(),
                rc.getRegion(),
                stops.size(),
                rc.getEstimatedMinutes(),
                rc.getTransportMode(),
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
                stops.size(),
                null,
                null,
                record.getDescription(),
                stops
        );
    }
}
