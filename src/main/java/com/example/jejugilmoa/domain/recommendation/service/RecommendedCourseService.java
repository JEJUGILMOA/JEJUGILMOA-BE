package com.example.jejugilmoa.domain.recommendation.service;

import com.example.jejugilmoa.domain.plan.enums.TravelTheme;
import com.example.jejugilmoa.domain.recommendation.dto.CourseWaypointItem;
import com.example.jejugilmoa.domain.recommendation.dto.RecommendedCourseDetailResponse;
import com.example.jejugilmoa.domain.recommendation.dto.RecommendedCourseResponse;
import com.example.jejugilmoa.domain.recommendation.entity.RecommendedCourse;
import com.example.jejugilmoa.domain.recommendation.entity.RecommendedCoursePath;
import com.example.jejugilmoa.domain.recommendation.exception.RecommendationErrorCode;
import com.example.jejugilmoa.domain.recommendation.repository.RecommendedCourseRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendedCourseService {

    private final RecommendedCourseRepository recommendedCourseRepository;

    public List<RecommendedCourseResponse> getRecommendedCourses(List<TravelTheme> themes) {
        List<RecommendedCourse> courses;
        if (themes == null || themes.isEmpty()) {
            courses = recommendedCourseRepository.findAllWithPathsOrderByCopyCountDesc();
        } else {
            List<String> themeNames = themes.stream().map(Enum::name).toList();
            courses = recommendedCourseRepository.findAllByThemeInWithPathsOrderByCopyCountDesc(themeNames);
        }
        return courses.stream().map(this::toResponse).toList();
    }

    public RecommendedCourseDetailResponse getCourseDetail(Long courseId) {
        RecommendedCourse course = recommendedCourseRepository.findByIdWithPaths(courseId)
                .orElseThrow(() -> new GeneralException(RecommendationErrorCode.COURSE_NOT_FOUND));

        List<RecommendedCourseDetailResponse.CourseStopItem> stops = course.getPaths().stream()
                .map(path -> new RecommendedCourseDetailResponse.CourseStopItem(
                        path.getSequenceOrder(),
                        path.getPlace().getId(),
                        path.getPlace().getName(),
                        path.getPlace().getImageUrl(),
                        path.getTravelTimeToNext()
                ))
                .toList();

        return new RecommendedCourseDetailResponse(
                course.getId(),
                course.getImageUrl(),
                course.getTitle(),
                course.getRegion(),
                course.getIsFree(),
                course.getRating(),
                course.getTransportMode(),
                course.getPaths().size(),
                course.getEstimatedMinutes(),
                course.getDescription(),
                stops
        );
    }

    private RecommendedCourseResponse toResponse(RecommendedCourse course) {
        List<CourseWaypointItem> waypoints = course.getPaths().stream()
                .map(path -> new CourseWaypointItem(
                        path.getSequenceOrder(),
                        path.getPlace().getId(),
                        path.getPlace().getName(),
                        path.getPlace().getImageUrl(),
                        path.getPlace().getLatitude(),
                        path.getPlace().getLongitude()
                ))
                .toList();
        return new RecommendedCourseResponse(
                course.getId(),
                course.getTitle(),
                course.getTheme(),
                course.getDescription(),
                course.getCopyCount(),
                waypoints
        );
    }
}
