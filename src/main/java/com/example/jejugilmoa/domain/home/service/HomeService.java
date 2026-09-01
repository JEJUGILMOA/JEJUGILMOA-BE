package com.example.jejugilmoa.domain.home.service;

import com.example.jejugilmoa.domain.home.dto.HomeCourseResponse;
import com.example.jejugilmoa.domain.home.dto.HomePlaceResponse;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.entity.PlaceHashtag;
import com.example.jejugilmoa.domain.place.entity.PopularPlace;
import com.example.jejugilmoa.domain.place.enums.CurationLabel;
import com.example.jejugilmoa.domain.place.repository.PlaceHashtagRepository;
import com.example.jejugilmoa.domain.place.repository.PopularPlaceRepository;
import com.example.jejugilmoa.domain.place.service.PlacePersistService;
import com.example.jejugilmoa.domain.recommendation.entity.RecommendedCourse;
import com.example.jejugilmoa.domain.recommendation.repository.RecommendedCourseRepository;
import com.example.jejugilmoa.global.external.tourapi.KorServiceClient;
import com.example.jejugilmoa.global.external.tourapi.dto.DetailCommonItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeService {

    private static final int HOME_PLACE_LIMIT = 5;
    private static final int HOME_COURSE_LIMIT = 5;
    private static final int PREVIEW_LIMIT = 3;

    private final PopularPlaceRepository popularPlaceRepository;
    private final PlaceHashtagRepository placeHashtagRepository;
    private final RecommendedCourseRepository recommendedCourseRepository;
    private final KorServiceClient korServiceClient;
    private final PlacePersistService placePersistService;

    public List<HomePlaceResponse> getHomePlaces() {
        LinkedHashSet<Long> seen = new LinkedHashSet<>();
        List<PopularPlace> collected = new ArrayList<>();

        popularPlaceRepository.findByCurationLabelWithPlace(
                CurationLabel.TODAY_PICK, PageRequest.of(0, HOME_PLACE_LIMIT)
        ).forEach(pp -> {
            if (seen.add(pp.getId())) collected.add(pp);
        });

        if (collected.size() < HOME_PLACE_LIMIT) {
            popularPlaceRepository.findByCurationLabelWithPlace(
                    CurationLabel.TRAVELER_PICK, PageRequest.of(0, HOME_PLACE_LIMIT)
            ).forEach(pp -> {
                if (seen.add(pp.getId()) && collected.size() < HOME_PLACE_LIMIT) collected.add(pp);
            });
        }

        if (collected.size() < HOME_PLACE_LIMIT) {
            popularPlaceRepository.findTopGeneralWithPlace(
                    PageRequest.of(0, HOME_PLACE_LIMIT)
            ).forEach(pp -> {
                if (seen.add(pp.getId()) && collected.size() < HOME_PLACE_LIMIT) collected.add(pp);
            });
        }

        List<Long> placeIds = collected.stream().map(pp -> pp.getPlace().getId()).toList();
        Map<Long, PlaceHashtag> hashtagMap = placeHashtagRepository.findByPlace_IdIn(placeIds)
            .stream().collect(Collectors.toMap(ht -> ht.getPlace().getId(), ht -> ht));

        Map<Long, String> descriptionMap = enrichDescriptions(collected);

        return collected.stream()
                .map(pp -> toHomePlaceResponse(pp, hashtagMap.get(pp.getPlace().getId()),
                        descriptionMap.getOrDefault(pp.getPlace().getId(), pp.getPlace().getDescription())))
                .toList();
    }

    @Transactional(readOnly = true)
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

    private Map<Long, String> enrichDescriptions(List<PopularPlace> pps) {
        Map<String, Long> needEnrichment = new LinkedHashMap<>();
        for (PopularPlace pp : pps) {
            Place place = pp.getPlace();
            if (place.getDescription() == null && place.getExternalId() != null) {
                needEnrichment.put(place.getExternalId(), place.getId());
            }
        }
        if (needEnrichment.isEmpty()) return Map.of();

        log.info("홈 장소 개요 보강 시작: {}건", needEnrichment.size());
        Map<String, String> overviews = new LinkedHashMap<>();
        for (String externalId : needEnrichment.keySet()) {
            DetailCommonItem common = korServiceClient.detailCommon2(externalId);
            if (common != null && common.overview() != null && !common.overview().isBlank()) {
                overviews.put(externalId, common.overview());
            }
        }
        if (!overviews.isEmpty()) {
            placePersistService.applyOverviews(overviews);
        }

        Map<Long, String> result = new HashMap<>();
        for (Map.Entry<String, String> e : overviews.entrySet()) {
            Long placeId = needEnrichment.get(e.getKey());
            if (placeId != null) result.put(placeId, e.getValue());
        }
        return result;
    }

    private HomePlaceResponse toHomePlaceResponse(PopularPlace pp, PlaceHashtag hashtag, String description) {
        var place = pp.getPlace();
        return new HomePlaceResponse(
                place.getId(),
                place.getName(),
                place.getCategory() != null ? place.getCategory().getName() : null,
                stripJejuPrefix(place.getAddress()),
                place.getImageUrl(),
                description,
                pp.getCurationLabel() != null ? pp.getCurationLabel().name() : null,
                pp.getRating(),
                buildHashtags(
                    hashtag != null ? hashtag.getMidLabel() : null,
                    hashtag != null ? hashtag.getSubLabel() : null
                )
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

    private static String stripJejuPrefix(String address) {
        if (address == null || address.isBlank()) return null;
        String stripped = address.replace("제주특별자치도", "").strip();
        return stripped.isBlank() ? null : stripped;
    }

    private static List<String> buildHashtags(String mid, String sub) {
        List<String> tags = new ArrayList<>();
        if (mid != null) tags.add(mid);
        if (sub != null) tags.add(sub);
        return tags.isEmpty() ? null : tags;
    }
}
