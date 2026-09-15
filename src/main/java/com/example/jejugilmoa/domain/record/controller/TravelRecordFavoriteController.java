package com.example.jejugilmoa.domain.record.controller;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.record.controller.docs.TravelRecordFavoriteControllerDocs;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCardResponse;
import com.example.jejugilmoa.domain.record.exception.RecordErrorCode;
import com.example.jejugilmoa.domain.record.service.TravelRecordFavoriteService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "기록 즐겨찾기", description = "여행 기록 즐겨찾기 API")
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class TravelRecordFavoriteController implements TravelRecordFavoriteControllerDocs {
    private final TravelRecordFavoriteService favoriteService;

    @PostMapping("/{recordId}/favorites")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Void> add(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long recordId) {
        favoriteService.add(principal.userId(), recordId);
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, null);
    }

    @DeleteMapping("/{recordId}/favorites")
    public ApiResponse<Void> delete(@AuthenticationPrincipal UserPrincipal principal, @PathVariable Long recordId) {
        favoriteService.delete(principal.userId(), recordId);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, null);
    }

    @GetMapping("/favorites")
    public ApiResponse<PageResponse<TravelRecordCardResponse>> list(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (page < 0) throw new GeneralException(RecordErrorCode.INVALID_PAGE);
        if (size < 1 || size > 100) throw new GeneralException(RecordErrorCode.INVALID_PAGE_SIZE);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK,
                favoriteService.list(principal.userId(), PageRequest.of(page, size)));
    }
}
