package com.example.jejugilmoa.domain.record.controller;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.record.controller.docs.TravelRecordControllerDocs;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCreateRequest;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCreateResponse;
import com.example.jejugilmoa.domain.record.dto.TravelRecordDetailResponse;
import com.example.jejugilmoa.domain.record.enums.RecordView;
import com.example.jejugilmoa.domain.record.exception.RecordErrorCode;
import com.example.jejugilmoa.domain.record.service.TravelRecordService;
import com.example.jejugilmoa.domain.record.service.TravelRecordQueryService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import com.example.jejugilmoa.global.apiPayload.dto.PageResponse;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "여행 기록", description = "완료 여행의 기록 생성")
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class TravelRecordController implements TravelRecordControllerDocs {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Sort RECORD_SORT = Sort.by(
            Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final TravelRecordService travelRecordService;
    private final TravelRecordQueryService travelRecordQueryService;

    @GetMapping
    public ApiResponse<PageResponse<?>> getRecords(
            @AuthenticationPrincipal UserPrincipal principal,
            @RequestParam(defaultValue = "CARD") RecordView view,
            @RequestParam(defaultValue = "false") boolean mine,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        validatePage(page, size);
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                travelRecordQueryService.getRecords(
                        principal.userId(), view, mine, PageRequest.of(page, size, RECORD_SORT)));
    }

    @GetMapping("/{recordId}")
    public ApiResponse<TravelRecordDetailResponse> getDetail(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long recordId) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.REQUEST_OK,
                travelRecordQueryService.getDetail(recordId, principal.userId()));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<TravelRecordCreateResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TravelRecordCreateRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.CREATED, travelRecordService.create(principal.userId(), request));
    }

    private void validatePage(int page, int size) {
        if (page < 0) {
            throw new GeneralException(RecordErrorCode.INVALID_PAGE);
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new GeneralException(RecordErrorCode.INVALID_PAGE_SIZE);
        }
    }
}
