package com.example.jejugilmoa.domain.place.service;

import com.example.jejugilmoa.domain.place.entity.Category;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.repository.CategoryRepository;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.place.repository.PopularPlaceRepository;
import com.example.jejugilmoa.global.external.tourapi.TourApiClient;
import com.example.jejugilmoa.global.external.tourapi.dto.TourListItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.PrecisionModel;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlacePersistServiceTest {

    @Mock PlaceRepository placeRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock PopularPlaceRepository popularPlaceRepository;
    @Spy GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    @InjectMocks PlacePersistService placePersistService;

    private static TourListItem item(String rlteTatsCd, String rlteTatsNm,
                                     String rlteSignguNm, String rlteCtgryLclsNm, int rank) {
        return new TourListItem(
            "tAtsCd-001", "성산일출봉",
            "50", "제주특별자치도", "50110", "제주시",
            rlteTatsCd, rlteTatsNm,
            "50", "제주특별자치도", "50110", rlteSignguNm,
            rlteCtgryLclsNm, null, null, rank
        );
    }

    @Test
    void saveItems_savesNewPlace() {
        var category = Category.builder().id(1L).name("자연").description("자연").build();
        var item = item("rlteCd-001", "한라산", "제주시", "관광지", 1);

        given(placeRepository.existsByExternalId("rlteCd-001")).willReturn(false);
        given(categoryRepository.findByName("자연")).willReturn(Optional.of(category));

        placePersistService.saveItems(TourApiClient.SIGNGU_JEJU_SI, List.of(item));

        var captor = ArgumentCaptor.forClass(Place.class);
        verify(placeRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("한라산");
        assertThat(captor.getValue().getExternalId()).isEqualTo("rlteCd-001");
    }

    @Test
    void saveItems_skipsExistingPlace() {
        var item = item("rlteCd-001", "한라산", "제주시", "관광지", 1);

        given(placeRepository.existsByExternalId("rlteCd-001")).willReturn(true);

        placePersistService.saveItems(TourApiClient.SIGNGU_JEJU_SI, List.of(item));

        verify(placeRepository, never()).save(any());
    }

    @Test
    void saveItems_skipsWhenCategoryNotFound() {
        var item = item("rlteCd-001", "한라산", "제주시", "관광지", 1);

        given(placeRepository.existsByExternalId("rlteCd-001")).willReturn(false);
        given(categoryRepository.findByName("자연")).willReturn(Optional.empty());

        placePersistService.saveItems(TourApiClient.SIGNGU_JEJU_SI, List.of(item));

        verify(placeRepository, never()).save(any());
    }

    @Test
    void saveItems_rank1_gives500VisitCount() {
        var category = Category.builder().id(1L).name("자연").description("자연").build();
        var item = item("rlteCd-001", "한라산", "제주시", "관광지", 1);

        given(placeRepository.existsByExternalId("rlteCd-001")).willReturn(false);
        given(categoryRepository.findByName("자연")).willReturn(Optional.of(category));
        given(placeRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        placePersistService.saveItems(TourApiClient.SIGNGU_JEJU_SI, List.of(item));

        var captor = ArgumentCaptor.forClass(com.example.jejugilmoa.domain.place.entity.PopularPlace.class);
        verify(popularPlaceRepository).save(captor.capture());
        assertThat(captor.getValue().getVisitCount()).isEqualTo(500);
    }
}
