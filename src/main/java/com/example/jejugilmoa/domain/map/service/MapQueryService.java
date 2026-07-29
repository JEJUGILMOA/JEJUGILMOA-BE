package com.example.jejugilmoa.domain.map.service;

import com.example.jejugilmoa.domain.map.converter.MapConverter;
import com.example.jejugilmoa.domain.map.dto.HeatmapCellDto;
import com.example.jejugilmoa.domain.map.dto.MapPlaceDto;
import com.example.jejugilmoa.domain.map.enums.CongestionLevel;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.place.repository.PopularPlaceRepository;
import com.example.jejugilmoa.domain.place.repository.PopularPlaceRepository.GridCellCount;
import com.example.jejugilmoa.domain.record.repository.TravelRecordPlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * 지도 뷰포트 내 장소 목록 조회 및 혼잡도 히트맵 집계를 담당합니다.
 * 혼잡도 전용 테이블(PLACE_CONGESTION, docs/architecture.md ERD상 미래 계획)이 아직 없어,
 * 이미 존재하는 두 신호를 임시로 조합해 혼잡도를 낸다:
 * 실제 방문 기록(TravelRecordPlace)을 우선 신호로, 방문 기록이 없는 격자는
 * 인기 지표(PopularPlace.visitCount)로 보완한다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MapQueryService {

    // 최근 N일 방문만 "현재 혼잡도" 신호로 인정 (오래된 방문 기록이 계속 반영되는 것을 방지)
    private static final int HEATMAP_RECENCY_DAYS = 90;
    // 응답에 포함된 셀들 중 최대 점수 대비 상대 비율로 2단계 분류 (목업 범례가 2단계뿐)
    private static final double CROWDED_THRESHOLD = 0.6;
    private static final double MODERATE_THRESHOLD = 0.25;

    private final PlaceRepository placeRepository;
    private final TravelRecordPlaceRepository travelRecordPlaceRepository;
    private final PopularPlaceRepository popularPlaceRepository;
    private final MapConverter mapConverter;

    public List<MapPlaceDto> getPlaces(
            double minLat, double maxLat, double minLng, double maxLng,
            String category, int limit) {
        var places = (category == null || category.isBlank())
            ? placeRepository.findWithinBounds(minLng, minLat, maxLng, maxLat, limit)
            : placeRepository.findWithinBoundsAndCategory(minLng, minLat, maxLng, maxLat, category, limit);
        return places.stream().map(mapConverter::toMapPlace).toList();
    }

    public List<HeatmapCellDto> getHeatmap(
            double minLat, double maxLat, double minLng, double maxLng, int gridSize) {
        // 두 신호를 각각 독립적인 네이티브 쿼리로 격자(row,col) 단위 점수로 집계한 뒤 여기서 병합한다.
        // (한 쿼리로 합치면 popular_place(1:1)와 travel_record_place(1:N)의 조인 팬아웃 때문에
        //  집계가 중복 카운트되는 버그가 생기기 쉬워, DB에서 합치지 않고 자바에서 합친다.)
        var since = LocalDateTime.now().minusDays(HEATMAP_RECENCY_DAYS);
        var actual = travelRecordPlaceRepository.aggregateVisitsByGrid(
            minLng, minLat, maxLng, maxLat, gridSize, since);
        var popular = popularPlaceRepository.aggregatePopularityByGrid(
            minLng, minLat, maxLng, maxLat, gridSize);

        Map<Long, Long> actualByCell = actual.stream()
            .collect(Collectors.toMap(c -> cellKey(c.getGridRow(), c.getGridCol()),
                TravelRecordPlaceRepository.GridCellCount::getScore));
        Map<Long, Long> popularByCell = popular.stream()
            .collect(Collectors.toMap(c -> cellKey(c.getGridRow(), c.getGridCol()), GridCellCount::getScore));

        // 셀별 폴백: 실제 방문 기록이 있으면 그 값을 쓰고, 없을 때만 인기 지표로 대체한다.
        // (두 신호를 더하지 않는 이유: 방문이 0건인 셀도 검색량만으로 부풀려지는 것을 막기 위함)
        Map<Long, Long> combined = new HashMap<>();
        Stream.concat(actualByCell.keySet().stream(), popularByCell.keySet().stream())
            .distinct()
            .forEach(key -> {
                long actualScore = actualByCell.getOrDefault(key, 0L);
                long popularScore = popularByCell.getOrDefault(key, 0L);
                combined.put(key, actualScore > 0 ? actualScore : popularScore);
            });

        // 절대 기준값이 없으므로(데이터 초기 단계) 이번 응답에 포함된 셀들의 최댓값 대비 상대 비율로 분류한다.
        long maxScore = combined.values().stream().mapToLong(Long::longValue).max().orElse(0L);
        if (maxScore == 0L) {
            return List.of();
        }

        return combined.entrySet().stream()
            .map(e -> toCellIfNotable(e.getKey(), e.getValue(), maxScore, gridSize, minLat, maxLat, minLng, maxLng))
            .filter(Objects::nonNull)
            .toList();
    }

    /**
     * 상대 비율이 MODERATE 미만인 셀은 응답에서 제외한다 — 조용한 지역은 블롭 없이
     * 빈 지도로 표현하는 목업(MAP-05) 의도를 그대로 따른 것.
     */
    private HeatmapCellDto toCellIfNotable(
            long key, long score, long maxScore, int gridSize,
            double minLat, double maxLat, double minLng, double maxLng) {
        double ratio = (double) score / maxScore;
        CongestionLevel level;
        if (ratio >= CROWDED_THRESHOLD) {
            level = CongestionLevel.CROWDED;
        } else if (ratio >= MODERATE_THRESHOLD) {
            level = CongestionLevel.MODERATE;
        } else {
            return null;
        }
        int row = (int) (key >> 32);
        int col = (int) (key & 0xFFFFFFFFL);
        return mapConverter.toCell(row, col, gridSize, minLat, maxLat, minLng, maxLng, level, ratio);
    }

    /**
     * (row, col)을 하나의 Long으로 패킹한 맵 키. row/col은 항상 [1, gridSize](≤20)의 작은 양수라
     * 상위 32비트에 row, 하위 32비트에 col을 넣어도 충돌하지 않는다.
     * 격자 좌표 전용의 작은 레코드 타입을 새로 만드는 대신 이 방식을 택함.
     */
    private long cellKey(int row, int col) {
        return ((long) row << 32) | (col & 0xFFFFFFFFL);
    }
}
