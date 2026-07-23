package com.example.jejugilmoa.domain.place.controller;

import com.example.jejugilmoa.domain.place.controller.docs.PlaceControllerDocs;
import com.example.jejugilmoa.domain.place.dto.PlaceDetailDto;
import com.example.jejugilmoa.domain.place.dto.PlaceSummaryDto;
import com.example.jejugilmoa.domain.place.dto.PopularPlaceDto;
import com.example.jejugilmoa.domain.place.service.PlaceQueryService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
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

    @GetMapping("/popular")
    public ApiResponse<List<PopularPlaceDto>> getPopular(
            @RequestParam(defaultValue = "20") int limit) {
        return ApiResponse.onSuccess(GeneralSuccessCode.FOUND, placeQueryService.getPopular(limit));
    }

    @GetMapping
    public ApiResponse<PageResponse<PlaceSummaryDto>> browse(
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.onSuccess(GeneralSuccessCode.FOUND,
            placeQueryService.browse(category, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    public ApiResponse<PlaceDetailDto> getDetail(@PathVariable Long id) {
        return ApiResponse.onSuccess(GeneralSuccessCode.FOUND, placeQueryService.getDetail(id));
    }
}
