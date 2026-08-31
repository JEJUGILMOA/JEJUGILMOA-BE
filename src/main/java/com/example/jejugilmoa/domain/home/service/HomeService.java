package com.example.jejugilmoa.domain.home.service;

import com.example.jejugilmoa.domain.home.dto.HomeCourseResponse;
import com.example.jejugilmoa.domain.home.dto.HomePlaceResponse;
import com.example.jejugilmoa.domain.place.entity.PopularPlace;
import com.example.jejugilmoa.domain.place.enums.CurationLabel;
import com.example.jejugilmoa.domain.place.repository.PopularPlaceRepository;
import com.example.jejugilmoa.domain.recommendation.entity.RecommendedCourse;
import com.example.jejugilmoa.domain.recommendation.repository.RecommendedCourseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private static final int HOME_PLACE_LIMIT = 5;
    private static final int HOME_COURSE_LIMIT = 5;
    private static final int PREVIEW_LIMIT = 3;

    private final PopularPlaceRepository popularPlaceRepository;
    private final RecommendedCourseRepository recommendedCourseRepository;

    public List<HomePlaceResponse> getHomePlaces() {
        LinkedHashSet<Long> seen = new LinkedHashSet<>();
        List<PopularPlace> collected = new ArrayList<>();

        // 1. TODAY_PICK 우선
        popularPlaceRepository.findByCurationLabelWithPlace(
                CurationLabel.TODAY_PICK, PageRequest.of(0, HOME_PLACE_LIMIT)
        ).forEach(pp -> {
            if (seen.add(pp.getId())) collected.add(pp);
        });

        // 2. TRAVELER_PICK
        if (collected.size() < HOME_PLACE_LIMIT) {
            popularPlaceRepository.findByCurationLabelWithPlace(
                    CurationLabel.TRAVELER_PICK, PageRequest.of(0, HOME_PLACE_LIMIT)
            ).forEach(pp -> {
                if (seen.add(pp.getId()) && collected.size() < HOME_PLACE_LIMIT) collected.add(pp);
            });
        }

        // 3. 일반 인기 장소 (curationLabel IS NULL)
        if (collected.size() < HOME_PLACE_LIMIT) {
            popularPlaceRepository.findTopGeneralWithPlace(
                    PageRequest.of(0, HOME_PLACE_LIMIT)
            ).forEach(pp -> {
                if (seen.add(pp.getId()) && collected.size() < HOME_PLACE_LIMIT) collected.add(pp);
            });
        }

        return collected.stream()
                .map(this::toHomePlaceResponse)
                .toList();
    }

    public List<HomeCourseResponse> getHomeCourses() {
        List<RecommendedCourse> courses = recommendedCourseRepository
                .findAllWithPathsOrderByCopyCountDesc()
                .stream()
                .limit(HOME_COURSE_LIMIT)
                .toList();

        return courses.stream()
                .map(this::toHomeCourseResponse)
                .toList();
    }

    private HomePlaceResponse toHomePlaceResponse(PopularPlace pp) {
        var place = pp.getPlace();

        return new HomePlaceResponse(
                place.getId(),
                place.getName(),
                place.getCategory() != null ? place.getCategory().getName() : null,
                extractRegion(place.getAddress()),
                place.getImageUrl(),
                place.getDescription(),
                pp.getCurationLabel() != null ? pp.getCurationLabel().name() : null,
                pp.getRating()
        );
    }

    private HomeCourseResponse toHomeCourseResponse(RecommendedCourse course) {
        List<HomeCourseResponse.CoursePreviewItem> preview = course.getPaths().stream()
                .limit(PREVIEW_LIMIT)
                .map(p -> new HomeCourseResponse.CoursePreviewItem(
                        p.getPlace().getId(),
                        p.getPlace().getImageUrl()
                ))
                .toList();

        List<String> tags = course.getTags() != null && !course.getTags().isBlank()
                ? Arrays.asList(course.getTags().split(","))
                : List.of();

        // 대표 이미지: imageUrl 없으면 첫 경유지 이미지 폴백
        String imageUrl = course.getImageUrl() != null
                ? course.getImageUrl()
                : preview.isEmpty() ? null : preview.get(0).imageUrl();

        return new HomeCourseResponse(
                course.getId(),
                imageUrl,
                course.getRegion(),
                course.getTitle(),
                course.getDescription(),
                tags,
                course.getEstimatedMinutes(),
                course.getPaths().size(),
                course.getTransportMode(),
                preview
        );
    }

    // address에서 시군구 추출 (예: "제주특별자치도 제주시 ..." → "제주시")
    private String extractRegion(String address) {
        if (address == null || address.isBlank()) return null;
        String[] parts = address.split("\\s+");
        for (String part : parts) {
            if (part.endsWith("시") || part.endsWith("군") || part.endsWith("구")) {
                return part;
            }
        }
        return parts.length > 1 ? parts[1] : parts[0];
    }
}
