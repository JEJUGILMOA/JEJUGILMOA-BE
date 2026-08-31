package com.example.jejugilmoa.global.controller;

import com.example.jejugilmoa.domain.place.service.PlaceSyncService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import com.example.jejugilmoa.global.scheduler.PopularPlaceSyncScheduler;
import com.example.jejugilmoa.global.scheduler.TodayPickSyncScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Profile("!prod")
@Tag(name = "[개발전용] 관리자 수동 동기화", description = "스케줄러를 즉시 실행하는 개발/테스트 전용 API")
@RestController
@RequestMapping("/dev/admin")
@RequiredArgsConstructor
public class DevAdminController {

    private final PlaceSyncService placeSyncService;
    private final TodayPickSyncScheduler todayPickSyncScheduler;
    private final PopularPlaceSyncScheduler popularPlaceSyncScheduler;

    @Operation(
        summary = "[개발전용] 장소 전체 동기화",
        description = "KorService2 areaBasedList2으로 place + popular_place 테이블을 동기화합니다. " +
                      "수백 건 API 호출이 발생하므로 수 분 이상 소요될 수 있습니다. " +
                      "TOUR_API_KEY 환경변수가 필요합니다."
    )
    @PostMapping("/sync/places")
    public ApiResponse<String> syncPlaces() {
        placeSyncService.syncFromKorService();
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, "장소 전체 동기화 완료");
    }

    @Operation(
        summary = "[개발전용] 장소 상세 정보 보강",
        description = "detailCommon2로 description(개요)을 채웁니다. " +
                      "장소 동기화 이후 실행해야 합니다. 50건 배치, 호출 간 100ms sleep."
    )
    @PostMapping("/sync/enrich")
    public ApiResponse<String> enrichPlaces() {
        placeSyncService.enrichPlaceDetails();
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, "장소 상세 정보 보강 완료");
    }

    @Operation(
        summary = "[개발전용] 오늘의 픽 동기화",
        description = "TatsCnctrRateService cnctrRate 상위 10건을 TODAY_PICK으로 표시합니다. " +
                      "TATS_CNCTR_RATE_API_KEY와 TATS_CNCTR_RATE_SIGNGU_CDS 환경변수가 필요합니다. " +
                      "미설정 시 경고 로그 후 아무 변경 없이 완료됩니다."
    )
    @PostMapping("/sync/today-pick")
    public ApiResponse<String> syncTodayPick() {
        todayPickSyncScheduler.syncTodayPick();
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, "오늘의 픽 동기화 완료");
    }

    @Operation(
        summary = "[개발전용] 주간 인기도 동기화",
        description = "KorService2 arrange=Q로 visitCount를 갱신하고, " +
                      "TarRlteTarService1으로 TRAVELER_PICK 상위 20건을 표시합니다."
    )
    @PostMapping("/sync/popular")
    public ApiResponse<String> syncPopular() {
        popularPlaceSyncScheduler.syncWeeklyPopularity();
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, "주간 인기도 동기화 완료");
    }
}
