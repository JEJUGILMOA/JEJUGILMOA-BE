package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.place.entity.Category;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.exception.PlaceErrorCode;
import com.example.jejugilmoa.domain.place.repository.CategoryRepository;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.plan.converter.TravelPlanConverter;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanCreateRequest;
import com.example.jejugilmoa.domain.plan.dto.TravelPlanCreateResponse;
import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.entity.TravelPlanPreference;
import com.example.jejugilmoa.domain.plan.exception.PlanErrorCode;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanPreferenceRepository;
import com.example.jejugilmoa.domain.plan.repository.TravelPlanRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.exception.UserErrorCode;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TravelPlanService {

    private final TravelPlanRepository travelPlanRepository;
    private final TravelPlanPreferenceRepository travelPlanPreferenceRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final CategoryRepository categoryRepository;

    @Transactional
    public TravelPlanCreateResponse create(Long userId, TravelPlanCreateRequest request) {
        if (request.startDate().isBefore(LocalDate.now()))
            throw new GeneralException(PlanErrorCode.INVALID_START_DATE);
        if (request.endDate().isBefore(request.startDate()))
            throw new GeneralException(PlanErrorCode.INVALID_DATE_RANGE);

        if (request.departurePlaceId() == null
                && (request.departureLocationName() == null || request.departureLocationName().isBlank()))
            throw new GeneralException(PlanErrorCode.DEPARTURE_REQUIRED);
        if (request.destinationPlaceId() == null
                && (request.destinationLocationName() == null || request.destinationLocationName().isBlank()))
            throw new GeneralException(PlanErrorCode.DESTINATION_REQUIRED);

        Place departurePlace = request.departurePlaceId() != null
                ? placeRepository.findByIdAndPublishedTrue(request.departurePlaceId())
                        .orElseThrow(() -> new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND))
                : null;
        Place destinationPlace = request.destinationPlaceId() != null
                ? placeRepository.findByIdAndPublishedTrue(request.destinationPlaceId())
                        .orElseThrow(() -> new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND))
                : null;

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));

        TravelPlan plan = TravelPlanConverter.toEntity(user, departurePlace, destinationPlace, request);
        TravelPlan saved = travelPlanRepository.save(plan);

        List<Category> categories = categoryRepository.findAllById(request.categoryIds());
        if (categories.size() != request.categoryIds().size())
            throw new GeneralException(PlanErrorCode.CATEGORY_NOT_FOUND);

        List<TravelPlanPreference> preferences = categories.stream()
                .map(cat -> TravelPlanPreference.builder()
                        .travelPlan(saved)
                        .category(cat)
                        .build())
                .toList();
        travelPlanPreferenceRepository.saveAll(preferences);

        return TravelPlanConverter.toCreateResponse(saved, categories);
    }
}
