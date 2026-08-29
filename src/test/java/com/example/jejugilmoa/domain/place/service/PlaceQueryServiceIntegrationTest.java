package com.example.jejugilmoa.domain.place.service;

import com.example.jejugilmoa.domain.place.entity.Category;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.repository.CategoryRepository;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class PlaceQueryServiceIntegrationTest {

    private static final String CATEGORY = "장소검색통합테스트";
    private static final String PUBLISHED_NAME = "제주 장소검색 공개";
    private static final String UNPUBLISHED_NAME = "제주 장소검색 비공개";
    private static final PageRequest PAGEABLE = PageRequest.of(0, 500);

    @Autowired PlaceQueryService placeQueryService;
    @Autowired PlaceRepository placeRepository;
    @Autowired CategoryRepository categoryRepository;

    @BeforeEach
    void setUp() {
        Category category = categoryRepository.save(Category.builder().name(CATEGORY).build());
        placeRepository.save(place(PUBLISHED_NAME, true, category));
        placeRepository.save(place(UNPUBLISHED_NAME, false, category));
        placeRepository.flush();
    }

    @Test
    void browseWithoutKeywordAndCategoryReturnsAllPublishedPlaces() {
        var result = placeQueryService.browse(null, null, PAGEABLE);

        assertThat(result.content()).extracting("name")
            .contains(PUBLISHED_NAME)
            .doesNotContain(UNPUBLISHED_NAME);
        assertThat(result.totalElements()).isEqualTo(placeRepository.findByPublishedTrue(PAGEABLE).getTotalElements());
    }

    @Test
    void browseWithKeywordOnlySearchesPublishedNamesAndAddresses() {
        var result = placeQueryService.browse("제주", null, PAGEABLE);

        assertThat(result.content()).extracting("name")
            .contains(PUBLISHED_NAME)
            .doesNotContain(UNPUBLISHED_NAME);
        assertThat(result.content()).allSatisfy(item -> {
            assertThat(item.name().contains("제주") || item.address().contains("제주")).isTrue();
        });
    }

    @Test
    void browseWithCategoryOnlySearchesPublishedPlacesInCategory() {
        var result = placeQueryService.browse(null, CATEGORY, PAGEABLE);

        assertThat(result.content()).extracting("name").containsExactly(PUBLISHED_NAME);
    }

    @Test
    void browseWithKeywordAndCategoryAppliesBothConditions() {
        var result = placeQueryService.browse("제주", CATEGORY, PAGEABLE);

        assertThat(result.content()).extracting("name").containsExactly(PUBLISHED_NAME);
    }

    @Test
    void browseTreatsLikeWildcardsAndEscapeCharacterAsLiterals() {
        Category category = categoryRepository.findByName(CATEGORY).orElseThrow();
        String name = "장소%_\\검색";
        placeRepository.saveAndFlush(place(name, true, category));

        assertThat(placeQueryService.browse("%", null, PAGEABLE).content()).extracting("name").contains(name);
        assertThat(placeQueryService.browse("_", null, PAGEABLE).content()).extracting("name").contains(name);
        assertThat(placeQueryService.browse("\\", null, PAGEABLE).content()).extracting("name").contains(name);
    }

    private Place place(String name, boolean published, Category category) {
        return Place.builder()
            .name(name)
            .address("제주특별자치도 테스트 주소")
            .latitude(new BigDecimal("33.49960000"))
            .longitude(new BigDecimal("126.53120000"))
            .category(category)
            .published(published)
            .build();
    }
}
