package com.example.jejugilmoa.domain.place.service;

import com.example.jejugilmoa.domain.place.entity.Category;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.repository.CategoryRepository;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.place.repository.PopularPlaceRepository;
import com.example.jejugilmoa.global.external.tourapi.TourApiClient;
import com.example.jejugilmoa.global.external.tourapi.TourApiException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaceSyncServiceTest {

    @Mock TourApiClient tourApiClient;
    @Mock PlaceRepository placeRepository;
    @Mock CategoryRepository categoryRepository;
    @Mock PopularPlaceRepository popularPlaceRepository;
    @Spy GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 4326);
    @InjectMocks PlaceSyncService placeSyncService;

    private static TourListItem relatedPlaceItem(String rlteTatsCd, String rlteTatsNm,
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
    void syncBySigngu_savesNewPlace() {
        var category = Category.builder().id(1L).name("자연").description("자연").build();
        var item = relatedPlaceItem("rlteCd-001", "한라산", "제주시", "관광지", 1);

        given(tourApiClient.getAreaBased(anyString(), anyInt(), anyInt()))
            .willReturn(List.of(item));
        given(placeRepository.existsByExternalId("rlteCd-001")).willReturn(false);
        given(categoryRepository.findByName("자연")).willReturn(Optional.of(category));

        placeSyncService.syncBySigngu(TourApiClient.SIGNGU_JEJU_SI);

        var captor = ArgumentCaptor.forClass(Place.class);
        verify(placeRepository).save(captor.capture());
        assertThat(captor.getValue().getName()).isEqualTo("한라산");
        assertThat(captor.getValue().getExternalId()).isEqualTo("rlteCd-001");
    }

    @Test
    void syncBySigngu_skipsExistingPlace() {
        var item = relatedPlaceItem("rlteCd-001", "한라산", "제주시", "관광지", 1);

        given(tourApiClient.getAreaBased(anyString(), anyInt(), anyInt()))
            .willReturn(List.of(item));
        given(placeRepository.existsByExternalId("rlteCd-001")).willReturn(true);

        placeSyncService.syncBySigngu(TourApiClient.SIGNGU_JEJU_SI);

        verify(placeRepository, never()).save(any());
    }

    @Test
    void syncBySigngu_propagatesTourApiException() {
        given(tourApiClient.getAreaBased(anyString(), anyInt(), anyInt()))
            .willThrow(new TourApiException("TourAPI 응답 실패: api=지역기반 연관관광지, resultMsg=SERVICE_KEY_IS_NOT_REGISTERED_ERROR"));

        assertThatThrownBy(() -> placeSyncService.syncBySigngu(TourApiClient.SIGNGU_JEJU_SI))
            .isInstanceOf(TourApiException.class);

        verify(placeRepository, never()).save(any());
    }

    @Test
    void syncAllCategories_continuesAfterSignguFailure() {
        var category = Category.builder().id(1L).name("자연").description("자연").build();
        var item = relatedPlaceItem("rlteCd-002", "성산일출봉", "서귀포시", "관광지", 1);

        given(tourApiClient.getAreaBased(anyString(), anyInt(), anyInt()))
            .willThrow(new TourApiException("TourAPI 응답 실패"))  // 제주시 실패
            .willReturn(List.of(item));                             // 서귀포시 성공
        given(placeRepository.existsByExternalId("rlteCd-002")).willReturn(false);
        given(categoryRepository.findByName("자연")).willReturn(Optional.of(category));

        placeSyncService.syncAllCategories();

        verify(placeRepository, times(1)).save(any());
    }
}
