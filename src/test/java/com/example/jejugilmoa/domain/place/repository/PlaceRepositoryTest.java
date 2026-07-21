package com.example.jejugilmoa.domain.place.repository;

import com.example.jejugilmoa.domain.place.entity.Category;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.global.config.JpaConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(JpaConfig.class)
@Transactional
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://${DB_HOST:localhost}:${DB_PORT:5433}/${DB_NAME:jejugilmoa}",
    "spring.datasource.username=${DB_USER:postgres}",
    "spring.datasource.password=${DB_PASSWORD:postgres}",
    "spring.datasource.driver-class-name=org.postgresql.Driver",
    "spring.jpa.hibernate.ddl-auto=update"
})
class PlaceRepositoryTest {

    @Autowired PlaceRepository placeRepository;
    @Autowired CategoryRepository categoryRepository;

    private static final GeometryFactory GF = new GeometryFactory(new PrecisionModel(), 4326);

    @BeforeEach
    void setUp() {
        placeRepository.deleteAllInBatch();
        categoryRepository.deleteAllInBatch();
    }

    private Category savedCategory() {
        return categoryRepository.save(
            Category.builder().name("자연").description("자연 관광지").build());
    }

    private Place buildPlace(String externalId, Category cat) {
        var geom = GF.createPoint(new Coordinate(126.5, 33.4));
        return Place.builder()
            .externalId(externalId)
            .name("테스트 장소")
            .address("제주시 테스트로 1")
            .latitude(new BigDecimal("33.40000000"))
            .longitude(new BigDecimal("126.50000000"))
            .geom(geom)
            .category(cat)
            .published(true)
            .build();
    }

    @Test
    void findByExternalId_returnsPlace() {
        var cat = savedCategory();
        placeRepository.save(buildPlace("content-001", cat));

        var result = placeRepository.findByExternalId("content-001");

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("테스트 장소");
    }

    @Test
    void findByCategoryNameAndPublishedTrue_returnsOnlyPublished() {
        var cat = savedCategory();
        placeRepository.save(buildPlace("c1", cat));
        placeRepository.save(Place.builder()
            .externalId("c2")
            .name("비공개 장소")
            .address("제주시 테스트로 2")
            .latitude(new BigDecimal("33.40000000"))
            .longitude(new BigDecimal("126.50000000"))
            .geom(GF.createPoint(new Coordinate(126.5, 33.4)))
            .category(cat)
            .published(false)
            .build());

        var page = placeRepository.findByCategoryNameAndPublishedTrue("자연", PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getExternalId()).isEqualTo("c1");
    }
}
