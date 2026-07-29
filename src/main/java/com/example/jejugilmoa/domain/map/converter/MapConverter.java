package com.example.jejugilmoa.domain.map.converter;

import com.example.jejugilmoa.domain.map.dto.HeatmapCellDto;
import com.example.jejugilmoa.domain.map.dto.MapPlaceDto;
import com.example.jejugilmoa.domain.map.enums.CongestionLevel;
import com.example.jejugilmoa.domain.place.entity.Place;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class MapConverter {

    public MapPlaceDto toMapPlace(Place p) {
        return new MapPlaceDto(
            p.getId(),
            p.getName(),
            p.getCategory() != null ? p.getCategory().getName() : null,
            p.getImageUrl(),
            p.getLatitude(),
            p.getLongitude()
        );
    }

    /**
     * gridRow/gridCol은 DB의 width_bucket이 1부터 매긴 격자 인덱스이므로,
     * 셀 중심 좌표는 (인덱스 - 0.5) * 셀 한 변의 길이 만큼 min 좌표에서 떨어진 지점이다.
     */
    public HeatmapCellDto toCell(
            int gridRow, int gridCol, int gridSize,
            double minLat, double maxLat, double minLng, double maxLng,
            CongestionLevel level, double intensity) {
        double cellLat = minLat + (gridRow - 0.5) * (maxLat - minLat) / gridSize;
        double cellLng = minLng + (gridCol - 0.5) * (maxLng - minLng) / gridSize;
        return new HeatmapCellDto(
            BigDecimal.valueOf(cellLat),
            BigDecimal.valueOf(cellLng),
            level,
            intensity
        );
    }
}
