package com.example.jejugilmoa.global.controller;

import com.example.jejugilmoa.domain.place.service.PlaceSyncService;
import com.example.jejugilmoa.global.apiPayload.ApiResponse;
import com.example.jejugilmoa.global.apiPayload.code.GeneralSuccessCode;
import com.example.jejugilmoa.global.scheduler.PopularPlaceSyncScheduler;
import com.example.jejugilmoa.global.scheduler.TodayPickSyncScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.*;

import java.util.concurrent.CompletableFuture;

@Slf4j
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
        CompletableFuture.runAsync(placeSyncService::syncFromKorService)
                .exceptionally(e -> { log.error("장소 전체 동기화 실패", e); return null; });
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK, "장소 전체 동기화를 백그라운드에서 시작했습니다. 서버 로그를 확인하세요.");
    }

    @Operation(
        summary = "[개발전용] 장소 500건 배치 추가",
        description = "지정한 pageNo의 KorService2 areaBasedList2 결과(500건)를 저장합니다. " +
                      "중복 장소는 자동 스킵됩니다. pageNo=1부터 시작해 매 호출마다 1씩 증가시켜 데이터를 누적하세요."
    )
    @PostMapping("/sync/places/batch")
    public ApiResponse<String> syncPlacesBatch(@RequestParam(defaultValue = "1") int pageNo) {
        int saved = placeSyncService.syncBatch(pageNo);
        return ApiResponse.onSuccess(GeneralSuccessCode.REQUEST_OK,
                "장소 배치 동기화 완료 (page=" + pageNo + "): " + saved + "건 신규 추가");
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
