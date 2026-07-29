package com.example.jejugilmoa.domain.map.controller;

import com.example.jejugilmoa.domain.map.controller.docs.MapControllerDocs;
import com.example.jejugilmoa.domain.map.dto.HeatmapCellDto;
import com.example.jejugilmoa.domain.map.dto.MapPlaceDto;
import com.example.jejugilmoa.domain.map.exception.MapErrorCode;
import com.example.jejugilmoa.domain.map.service.MapQueryService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "지도", description = "지도 마커 및 인기 지역 혼잡도 히트맵 조회")
@RestController
@RequestMapping("/api/map")
@RequiredArgsConstructor
public class MapController implements MapControllerDocs {

    private static final int MAX_LIMIT = 500;
    private static final int MAX_GRID_SIZE = 20;

    private final MapQueryService mapQueryService;

    @GetMapping("/places")
    public ApiResponse<List<MapPlaceDto>> getPlaces(
            @RequestParam double minLat,
            @RequestParam double maxLat,
            @RequestParam double minLng,
            @RequestParam double maxLng,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "200") int limit) {
        validateBounds(minLat, maxLat, minLng, maxLng);
        if (limit < 1 || limit > MAX_LIMIT) throw new GeneralException(MapErrorCode.INVALID_LIMIT);
        return ApiResponse.onSuccess(GeneralSuccessCode.FOUND,
            mapQueryService.getPlaces(minLat, maxLat, minLng, maxLng, category, limit));
    }

    @GetMapping("/heatmap")
    public ApiResponse<List<HeatmapCellDto>> getHeatmap(
            @RequestParam double minLat,
            @RequestParam double maxLat,
            @RequestParam double minLng,
            @RequestParam double maxLng,
            @RequestParam(defaultValue = "10") int gridSize) {
        validateBounds(minLat, maxLat, minLng, maxLng);
        if (gridSize < 1 || gridSize > MAX_GRID_SIZE) throw new GeneralException(MapErrorCode.INVALID_GRID_SIZE);
        return ApiResponse.onSuccess(GeneralSuccessCode.FOUND,
            mapQueryService.getHeatmap(minLat, maxLat, minLng, maxLng, gridSize));
    }

    private void validateBounds(double minLat, double maxLat, double minLng, double maxLng) {
        if (minLat >= maxLat || minLng >= maxLng) throw new GeneralException(MapErrorCode.INVALID_BOUNDS);
    }
}
