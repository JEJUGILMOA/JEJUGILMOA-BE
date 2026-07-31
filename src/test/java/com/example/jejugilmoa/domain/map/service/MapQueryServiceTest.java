package com.example.jejugilmoa.domain.map.service;

import com.example.jejugilmoa.domain.map.converter.MapConverter;
import com.example.jejugilmoa.domain.map.dto.HeatmapCellDto;
import com.example.jejugilmoa.domain.map.dto.MapPlaceDto;
import com.example.jejugilmoa.domain.map.enums.CongestionLevel;
import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.place.repository.PopularPlaceRepository;
import com.example.jejugilmoa.domain.record.repository.TravelRecordPlaceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class MapQueryServiceTest {

    @Mock PlaceRepository placeRepository;
    @Mock TravelRecordPlaceRepository travelRecordPlaceRepository;
    @Mock PopularPlaceRepository popularPlaceRepository;
    @Mock MapConverter mapConverter;
    @InjectMocks MapQueryService mapQueryService;

    @Test
    void getPlaces_usesUnfilteredQuery_whenCategoryBlank() {
        var place = samplePlace();
        given(placeRepository.findWithinBounds(126.1, 33.2, 126.9, 33.6, 200))
            .willReturn(List.of(place));
        given(mapConverter.toMapPlace(place))
            .willReturn(new MapPlaceDto(1L, "성산일출봉", "자연", "img.jpg", place.getLatitude(), place.getLongitude()));

        var result = mapQueryService.getPlaces(33.2, 33.6, 126.1, 126.9, null, 200);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("성산일출봉");
    }

    @Test
    void getPlaces_usesCategoryQuery_whenCategoryGiven() {
        var place = samplePlace();
        given(placeRepository.findWithinBoundsAndCategory(126.1, 33.2, 126.9, 33.6, "자연", 200))
            .willReturn(List.of(place));
        given(mapConverter.toMapPlace(place))
            .willReturn(new MapPlaceDto(1L, "성산일출봉", "자연", "img.jpg", place.getLatitude(), place.getLongitude()));

        var result = mapQueryService.getPlaces(33.2, 33.6, 126.1, 126.9, "자연", 200);

        assertThat(result).hasSize(1);
    }

    @Test
    void getHeatmap_returnsEmpty_whenNoSignals() {
        given(travelRecordPlaceRepository.aggregateVisitsByGrid(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt(), any()))
            .willReturn(List.of());
        given(popularPlaceRepository.aggregatePopularityByGrid(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
            .willReturn(List.of());

        var result = mapQueryService.getHeatmap(33.2, 33.6, 126.1, 126.9, 10);

        assertThat(result).isEmpty();
    }

    @Test
    void getHeatmap_actualVisitsTakePrecedenceOverPopularFallback() {
        var actualCell = actualCell(1, 1, 10L);
        var popularCellSameKey = popularCell(1, 1, 1L); // should be ignored since actual > 0
        var popularOnlyCell = popularCell(2, 2, 4L); // no actual data -> falls back to popular

        given(travelRecordPlaceRepository.aggregateVisitsByGrid(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt(), any()))
            .willReturn(List.of(actualCell));
        given(popularPlaceRepository.aggregatePopularityByGrid(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
            .willReturn(List.of(popularCellSameKey, popularOnlyCell));
        given(mapConverter.toCell(anyInt(), anyInt(), anyInt(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                any(CongestionLevel.class), anyDouble()))
            .willAnswer(inv -> new HeatmapCellDto(BigDecimal.ZERO, BigDecimal.ZERO, inv.getArgument(7), inv.getArgument(8)));

        var result = mapQueryService.getHeatmap(33.2, 33.6, 126.1, 126.9, 10);

        // combined scores: cell(1,1)=10 (actual wins over popular=1), cell(2,2)=4 (popular fallback)
        // maxScore=10 -> ratio(1,1)=1.0 >= 0.6 => CROWDED, ratio(2,2)=0.4 >= 0.25 => MODERATE
        assertThat(result).hasSize(2);
        assertThat(result).extracting(HeatmapCellDto::level)
            .containsExactlyInAnyOrder(CongestionLevel.CROWDED, CongestionLevel.MODERATE);
    }

    @Test
    void getHeatmap_excludesCellsBelowModerateThreshold() {
        var crowded = actualCell(1, 1, 100L);
        var belowThreshold = actualCell(3, 3, 20L); // ratio 0.2 < 0.25 -> excluded

        given(travelRecordPlaceRepository.aggregateVisitsByGrid(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt(), any()))
            .willReturn(List.of(crowded, belowThreshold));
        given(popularPlaceRepository.aggregatePopularityByGrid(anyDouble(), anyDouble(), anyDouble(), anyDouble(), anyInt()))
            .willReturn(List.of());
        given(mapConverter.toCell(anyInt(), anyInt(), anyInt(), anyDouble(), anyDouble(), anyDouble(), anyDouble(),
                any(CongestionLevel.class), anyDouble()))
            .willAnswer(inv -> new HeatmapCellDto(BigDecimal.ZERO, BigDecimal.ZERO, inv.getArgument(7), inv.getArgument(8)));

        var result = mapQueryService.getHeatmap(33.2, 33.6, 126.1, 126.9, 10);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).level()).isEqualTo(CongestionLevel.CROWDED);
    }

    private Place samplePlace() {
        return Place.builder().id(1L).name("성산일출봉").imageUrl("img.jpg")
            .externalId("c1").address("서귀포시")
            .latitude(new BigDecimal("33.4589")).longitude(new BigDecimal("126.9425")).build();
    }

    private TravelRecordPlaceRepository.GridCellCount actualCell(int row, int col, long score) {
        return new TravelRecordPlaceRepository.GridCellCount() {
            public Integer getGridRow() { return row; }
            public Integer getGridCol() { return col; }
            public Long getScore() { return score; }
        };
    }

    private PopularPlaceRepository.GridCellCount popularCell(int row, int col, long score) {
        return new PopularPlaceRepository.GridCellCount() {
            public Integer getGridRow() { return row; }
            public Integer getGridCol() { return col; }
            public Long getScore() { return score; }
        };
    }
}
