package com.example.jejugilmoa.domain.plan.controller;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.plan.controller.docs.FavoriteControllerDocs;
import com.example.jejugilmoa.domain.plan.dto.FavoriteCreateRequest;
import com.example.jejugilmoa.domain.plan.dto.FavoritePlaceResponse;
import com.example.jejugilmoa.domain.plan.exception.FavoriteErrorCode;
import com.example.jejugilmoa.domain.plan.service.FavoriteService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "즐겨찾기", description = "장소 즐겨찾기 추가, 삭제 및 목록 조회 API")
@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController implements FavoriteControllerDocs {

    private static final int MAX_SIZE = 100;

    private final FavoriteService favoriteService;

    @Override
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> add(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody FavoriteCreateRequest request) {
        favoriteService.add(principal.userId(), request);
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, null);
    }

    @Override
    @DeleteMapping("/{placeId}")
    public ApiResponse<Void> delete(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long placeId) {
        favoriteService.delete(principal.userId(), placeId);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }

    @Override
    @GetMapping
    public ApiResponse<PageResponse<FavoritePlaceResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0) throw new GeneralException(FavoriteErrorCode.INVALID_PAGE);
        if (size < 1 || size > MAX_SIZE) throw new GeneralException(FavoriteErrorCode.INVALID_PAGE_SIZE);
        return ApiResponse.onSuccess(
                GeneralSuccessCode.FOUND,
                favoriteService.list(principal.userId(), PageRequest.of(page, size))
        );
    }
}
