package com.example.jejugilmoa.domain.place.service;

import com.example.jejugilmoa.domain.place.converter.PlaceConverter;
import com.example.jejugilmoa.domain.place.dto.PlaceDetailDto;
import com.example.jejugilmoa.domain.place.dto.PopularPlaceDto;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.entity.PopularPlace;
import com.example.jejugilmoa.domain.place.exception.PlaceErrorCode;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.place.repository.PopularPlaceRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaceQueryServiceTest {

    @Mock PlaceRepository placeRepository;
    @Mock PopularPlaceRepository popularPlaceRepository;
    @Mock PlaceConverter placeConverter;
    @InjectMocks PlaceQueryService placeQueryService;

    @Test
    void getPopular_returnsTopByVisitCount() {
        var place = Place.builder().id(1L).name("한라산").imageUrl("img.jpg")
            .externalId("c1").address("").latitude(BigDecimal.ZERO).longitude(BigDecimal.ZERO).build();
        var popular = PopularPlace.builder().place(place).visitCount(1000).build();
        given(popularPlaceRepository.findAllByOrderByVisitCountDesc(any()))
            .willReturn(List.of(popular));
        given(placeConverter.toPopular(popular))
            .willReturn(new PopularPlaceDto(1L, "한라산", "img.jpg", 1000));

        var result = placeQueryService.getPopular(3);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("한라산");
    }

    @Test
    void getDetail_throwsWhenNotFound() {
        given(placeRepository.findByIdAndPublishedTrue(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> placeQueryService.getDetail(999L))
            .isInstanceOf(GeneralException.class)
            .extracting(e -> ((GeneralException) e).getCode())
            .isEqualTo(PlaceErrorCode.PLACE_NOT_FOUND);
    }

    @Test
    void getDetail_throwsWhenUnpublished() {
        given(placeRepository.findByIdAndPublishedTrue(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> placeQueryService.getDetail(2L))
            .isInstanceOf(GeneralException.class)
            .extracting(e -> ((GeneralException) e).getCode())
            .isEqualTo(PlaceErrorCode.PLACE_NOT_FOUND);
    }

    @Test
    void browse_escapesLikeWildcardsInKeyword() {
        given(placeRepository.search(any(), any(), any())).willReturn(Page.empty());

        placeQueryService.browse("%카페_", null, PageRequest.of(0, 10));

        verify(placeRepository).search(eq("!%카페!_"), isNull(), any());
    }

    @Test
    void getDetail_returnsPlaceData() {
        var place = Place.builder().id(1L).name("한라산").imageUrl("img.jpg")
            .externalId("c1").address("제주시").latitude(new BigDecimal("33.36")).longitude(new BigDecimal("126.53")).build();
        var expected = new PlaceDetailDto(1L, "한라산", "제주시", new BigDecimal("33.36"), new BigDecimal("126.53"),
            null, "img.jpg", List.of(), "자연", null, null, null);

        given(placeRepository.findByIdAndPublishedTrue(1L)).willReturn(Optional.of(place));
        given(placeConverter.toDetail(place)).willReturn(expected);

        var result = placeQueryService.getDetail(1L);

        assertThat(result.name()).isEqualTo("한라산");
        assertThat(result.address()).isEqualTo("제주시");
    }
}
