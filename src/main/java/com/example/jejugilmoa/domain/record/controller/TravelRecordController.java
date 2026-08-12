package com.example.jejugilmoa.domain.record.controller;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.record.controller.docs.TravelRecordControllerDocs;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCreateRequest;
import com.example.jejugilmoa.domain.record.dto.TravelRecordCreateResponse;
import com.example.jejugilmoa.domain.record.service.TravelRecordService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "여행 기록", description = "완료 여행의 기록 생성")
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class TravelRecordController implements TravelRecordControllerDocs {

    private final TravelRecordService travelRecordService;

    @PostMapping
    public ApiResponse<TravelRecordCreateResponse> create(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody TravelRecordCreateRequest request) {
        return ApiResponse.onSuccess(
                GeneralSuccessCode.CREATED, travelRecordService.create(principal.userId(), request));
    }
}
