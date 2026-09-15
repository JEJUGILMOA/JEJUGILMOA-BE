package com.example.jejugilmoa.domain.report.controller;

import com.example.jejugilmoa.domain.auth.jwt.UserPrincipal;
import com.example.jejugilmoa.domain.report.controller.docs.ReportControllerDocs;
import com.example.jejugilmoa.domain.report.dto.ReportCreateRequest;
import com.example.jejugilmoa.domain.report.service.ReportService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class ReportController implements ReportControllerDocs {

    private final ReportService reportService;

    @PostMapping("/{recordId}/reports")
    public ApiResponse<Void> reportRecord(
            @PathVariable Long recordId,
            @RequestBody @Valid ReportCreateRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        reportService.reportRecord(recordId, principal.userId(), request);
        return ApiResponse.onSuccess(GeneralSuccessCode.CREATED, null);
    }
}
