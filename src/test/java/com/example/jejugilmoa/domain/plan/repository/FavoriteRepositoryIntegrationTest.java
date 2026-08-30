package com.example.jejugilmoa.domain.plan.repository;

import com.example.jejugilmoa.domain.place.entity.Category;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.repository.CategoryRepository;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.plan.entity.Favorite;
import com.example.jejugilmoa.domain.user.entity.User;
import com.example.jejugilmoa.domain.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class FavoriteRepositoryIntegrationTest {

    @Autowired FavoriteRepository favoriteRepository;
    @Autowired UserRepository userRepository;
    @Autowired PlaceRepository placeRepository;
    @Autowired CategoryRepository categoryRepository;

    @Test
    void findPublishedFavorites_returnsOnlyCurrentUsersPublishedPlacesInIdDescendingOrder() {
        User owner = saveUser("즐겨찾기 소유자");
        User another = saveUser("다른 사용자");
        Category category = categoryRepository.saveAndFlush(Category.builder().name("즐겨찾기 통합 테스트").build());
        Place olderPublished = savePlace("공개 장소 1", true, category);
        Place newerPublished = savePlace("공개 장소 2", true, category);
        Place unpublished = savePlace("비공개 장소", false, category);

        Favorite older = favoriteRepository.saveAndFlush(Favorite.builder().user(owner).place(olderPublished).build());
        Favorite newer = favoriteRepository.saveAndFlush(Favorite.builder().user(owner).place(newerPublished).build());
        favoriteRepository.saveAndFlush(Favorite.builder().user(owner).place(unpublished).build());
        favoriteRepository.saveAndFlush(Favorite.builder().user(another).place(olderPublished).build());

        var result = favoriteRepository
                .findAllByUserIdAndUserDeletedAtIsNullAndPlacePublishedTrueOrderByIdDesc(
                        owner.getId(), PageRequest.of(0, 20));

        assertThat(result.getContent()).extracting(Favorite::getId)
                .containsExactly(newer.getId(), older.getId());
        assertThat(result.getTotalElements()).isEqualTo(2);
        assertThat(result.getContent()).allSatisfy(favorite -> {
            assertThat(favorite.getUser().getId()).isEqualTo(owner.getId());
            assertThat(favorite.getPlace().isPublished()).isTrue();
            assertThat(favorite.getPlace().getCategory().getName()).isEqualTo(category.getName());
        });
    }

    private User saveUser(String nickname) {
        return userRepository.saveAndFlush(User.builder().nickname(nickname).build());
    }

    private Place savePlace(String name, boolean published, Category category) {
        return placeRepository.saveAndFlush(Place.builder()
                .name(name)
                .address("제주특별자치도")
                .latitude(new BigDecimal("33.49960000"))
                .longitude(new BigDecimal("126.53120000"))
                .category(category)
                .published(published)
                .build());
    }
}
