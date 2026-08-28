package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.exception.PlaceErrorCode;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.plan.converter.FavoriteConverter;
import com.example.jejugilmoa.domain.plan.dto.FavoriteCreateRequest;
import com.example.jejugilmoa.domain.plan.dto.FavoritePlaceResponse;
import com.example.jejugilmoa.domain.plan.entity.Favorite;
import com.example.jejugilmoa.domain.plan.exception.FavoriteErrorCode;
import com.example.jejugilmoa.domain.plan.repository.FavoriteRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.exception.UserErrorCode;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {

    private static final String FAVORITE_UNIQUE_CONSTRAINT = "uk_favorite";

    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;

    @Transactional
    public void add(Long userId, FavoriteCreateRequest request) {
        if (favoriteRepository.existsByUserIdAndPlaceId(userId, request.placeId())) {
            throw new GeneralException(FavoriteErrorCode.FAVORITE_ALREADY_EXISTS);
        }

        User user = userRepository.findByIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new GeneralException(UserErrorCode.USER_NOT_FOUND));
        Place place = placeRepository.findByIdAndPublishedTrue(request.placeId())
                .orElseThrow(() -> new GeneralException(PlaceErrorCode.PLACE_NOT_FOUND));

        Favorite favorite = Favorite.builder()
                .user(user)
                .place(place)
                .build();
        try {
            favoriteRepository.saveAndFlush(favorite);
        } catch (DataIntegrityViolationException exception) {
            if (hasConstraint(exception, FAVORITE_UNIQUE_CONSTRAINT)) {
                throw new GeneralException(FavoriteErrorCode.FAVORITE_ALREADY_EXISTS);
            }
            throw exception;
        }
    }

    @Transactional
    public void delete(Long userId, Long placeId) {
        Favorite favorite = favoriteRepository.findByUserIdAndPlaceId(userId, placeId)
                .orElseThrow(() -> new GeneralException(FavoriteErrorCode.FAVORITE_NOT_FOUND));
        favoriteRepository.delete(favorite);
    }

    public PageResponse<FavoritePlaceResponse> list(Long userId, Pageable pageable) {
        return PageResponse.of(favoriteRepository
                .findAllByUserIdAndUserDeletedAtIsNullAndPlacePublishedTrueOrderByIdDesc(userId, pageable)
                .map(FavoriteConverter::toResponse));
    }

    private boolean hasConstraint(Throwable throwable, String constraintName) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ConstraintViolationException constraintViolation
                    && constraintName.equals(constraintViolation.getConstraintName())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
