package com.example.jejugilmoa.domain.plan.service;

import com.example.jejugilmoa.domain.place.entity.Category;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.exception.PlaceErrorCode;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.plan.dto.FavoriteCreateRequest;
import com.example.jejugilmoa.domain.plan.entity.Favorite;
import com.example.jejugilmoa.domain.plan.exception.FavoriteErrorCode;
import com.example.jejugilmoa.domain.plan.repository.FavoriteRepository;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class FavoriteServiceTest {

    private static final Long USER_ID = 1L;
    private static final Long PLACE_ID = 10L;

    @Mock FavoriteRepository favoriteRepository;
    @Mock UserRepository userRepository;
    @Mock PlaceRepository placeRepository;
    @InjectMocks FavoriteService favoriteService;

    @Test
    void add_savesFavorite() {
        User user = User.builder().id(USER_ID).nickname("여행자").build();
        Place place = Place.builder().id(PLACE_ID).published(true).build();
        FavoriteCreateRequest request = new FavoriteCreateRequest(PLACE_ID);
        given(favoriteRepository.existsByUserIdAndPlaceId(USER_ID, PLACE_ID)).willReturn(false);
        given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(user));
        given(placeRepository.findByIdAndPublishedTrue(PLACE_ID)).willReturn(Optional.of(place));

        favoriteService.add(USER_ID, request);

        then(favoriteRepository).should().saveAndFlush(any(Favorite.class));
    }

    @Test
    void add_throwsPlaceNotFoundWhenPlaceMissingOrUnpublished() {
        User user = User.builder().id(USER_ID).nickname("여행자").build();
        FavoriteCreateRequest request = new FavoriteCreateRequest(PLACE_ID);
        given(favoriteRepository.existsByUserIdAndPlaceId(USER_ID, PLACE_ID)).willReturn(false);
        given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(user));
        given(placeRepository.findByIdAndPublishedTrue(PLACE_ID)).willReturn(Optional.empty());

        assertCode(() -> favoriteService.add(USER_ID, request), PlaceErrorCode.PLACE_NOT_FOUND);
        then(favoriteRepository).should(never()).saveAndFlush(any());
    }

    @Test
    void add_throwsWhenAlreadyFavorited() {
        FavoriteCreateRequest request = new FavoriteCreateRequest(PLACE_ID);
        given(favoriteRepository.existsByUserIdAndPlaceId(USER_ID, PLACE_ID)).willReturn(true);

        assertCode(() -> favoriteService.add(USER_ID, request), FavoriteErrorCode.FAVORITE_ALREADY_EXISTS);
        then(userRepository).shouldHaveNoInteractions();
        then(placeRepository).shouldHaveNoInteractions();
    }

    @Test
    void add_translatesUniqueConstraintConflictToDuplicateError() {
        User user = User.builder().id(USER_ID).nickname("여행자").build();
        Place place = Place.builder().id(PLACE_ID).published(true).build();
        FavoriteCreateRequest request = new FavoriteCreateRequest(PLACE_ID);
        given(favoriteRepository.existsByUserIdAndPlaceId(USER_ID, PLACE_ID)).willReturn(false);
        given(userRepository.findByIdAndDeletedAtIsNull(USER_ID)).willReturn(Optional.of(user));
        given(placeRepository.findByIdAndPublishedTrue(PLACE_ID)).willReturn(Optional.of(place));
        given(favoriteRepository.saveAndFlush(any(Favorite.class)))
                .willThrow(new DataIntegrityViolationException("uk_favorite"));

        assertCode(() -> favoriteService.add(USER_ID, request), FavoriteErrorCode.FAVORITE_ALREADY_EXISTS);
    }

    @Test
    void delete_deletesOwnedFavorite() {
        Favorite favorite = Favorite.builder().id(100L).build();
        given(favoriteRepository.findByUserIdAndPlaceId(USER_ID, PLACE_ID)).willReturn(Optional.of(favorite));

        favoriteService.delete(USER_ID, PLACE_ID);

        then(favoriteRepository).should().delete(favorite);
    }

    @Test
    void delete_throwsWhenFavoriteMissing() {
        given(favoriteRepository.findByUserIdAndPlaceId(USER_ID, PLACE_ID)).willReturn(Optional.empty());

        assertCode(() -> favoriteService.delete(USER_ID, PLACE_ID), FavoriteErrorCode.FAVORITE_NOT_FOUND);
        then(favoriteRepository).should(never()).delete(any());
    }

    @Test
    void delete_doesNotDeleteAnotherUsersFavorite() {
        given(favoriteRepository.findByUserIdAndPlaceId(USER_ID, PLACE_ID)).willReturn(Optional.empty());

        assertCode(() -> favoriteService.delete(USER_ID, PLACE_ID), FavoriteErrorCode.FAVORITE_NOT_FOUND);
        then(favoriteRepository).should(never()).delete(any());
    }

    @Test
    void list_returnsCurrentUsersPublishedFavoritesAsPageInRepositoryOrder() {
        Category category = Category.builder().id(3L).name("관광지").build();
        Place recentPlace = Place.builder().id(20L).name("성산일출봉").address("서귀포시")
                .imageUrl("recent.jpg").category(category).published(true).build();
        Place olderPlace = Place.builder().id(10L).name("한라산").address("제주시")
                .imageUrl("older.jpg").category(category).published(true).build();
        User user = User.builder().id(USER_ID).nickname("여행자").build();
        Favorite recent = Favorite.builder().id(2L).user(user).place(recentPlace).build();
        Favorite older = Favorite.builder().id(1L).user(user).place(olderPlace).build();
        PageRequest pageable = PageRequest.of(0, 20);
        given(favoriteRepository.findAllByUserIdAndUserDeletedAtIsNullAndPlacePublishedTrueOrderByIdDesc(
                USER_ID, pageable)).willReturn(new PageImpl<>(List.of(recent, older), pageable, 2));

        var result = favoriteService.list(USER_ID, pageable);

        assertThat(result.content()).extracting("placeId").containsExactly(20L, 10L);
        assertThat(result.content().get(0).category()).isEqualTo("관광지");
        assertThat(result.content().get(0).imageUrl()).isEqualTo("recent.jpg");
        assertThat(result.totalElements()).isEqualTo(2);
        then(favoriteRepository).should()
                .findAllByUserIdAndUserDeletedAtIsNullAndPlacePublishedTrueOrderByIdDesc(USER_ID, pageable);
    }

    private void assertCode(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, Object expectedCode) {
        assertThatThrownBy(callable)
                .isInstanceOf(GeneralException.class)
                .satisfies(exception -> assertThat(((GeneralException) exception).getCode()).isEqualTo(expectedCode));
    }
}
