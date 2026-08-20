package com.example.jejugilmoa.domain.place.controller;

import com.example.jejugilmoa.domain.place.controller.docs.PlaceControllerDocs;
import com.example.jejugilmoa.domain.place.dto.PlaceDetailDto;
import com.example.jejugilmoa.domain.place.dto.PlaceSummaryDto;
import com.example.jejugilmoa.domain.place.dto.PopularPlaceDto;
import com.example.jejugilmoa.domain.place.exception.PlaceErrorCode;
import com.example.jejugilmoa.domain.place.service.PlaceQueryService;
import com.example.jejugilmoa.domain.place.service.PlaceSyncService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "장소", description = "관광지 탐색 및 인기 장소 조회")
@RestController
@RequestMapping("/api/places")
@RequiredArgsConstructor
public class PlaceController implements PlaceControllerDocs {

    private final PlaceQueryService placeQueryService;
    private final PlaceSyncService placeSyncService;

    private static final int MAX_SIZE = 100;
    private static final int MAX_LIMIT = 100;

    @GetMapping("/popular")
    public ApiResponse<List<PopularPlaceDto>> getPopular(
            @RequestParam(defaultValue = "20") int limit) {
        if (limit < 1 || limit > MAX_LIMIT) throw new GeneralException(PlaceErrorCode.INVALID_LIMIT);
        return ApiResponse.onSuccess(GeneralSuccessCode.FOUND, placeQueryService.getPopular(limit));
    }

    @GetMapping
    public ApiResponse<PageResponse<PlaceSummaryDto>> browse(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0) throw new GeneralException(PlaceErrorCode.INVALID_PAGE);
        if (size < 1 || size > MAX_SIZE) throw new GeneralException(PlaceErrorCode.INVALID_PAGE_SIZE);
        return ApiResponse.onSuccess(GeneralSuccessCode.FOUND,
            placeQueryService.browse(keyword, category, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<PlaceDetailDto> getDetail(@PathVariable Long id) {
        return ApiResponse.onSuccess(GeneralSuccessCode.FOUND, placeQueryService.getDetail(id));
    }

    @PostMapping("/sync")
    public ApiResponse<Void> syncPlaces() {
        placeSyncService.syncAllCategories();
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }
}
