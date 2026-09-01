package com.example.jejugilmoa.global.scheduler;

import com.example.jejugilmoa.domain.place.enums.CurationLabel;
import com.example.jejugilmoa.domain.place.repository.PopularPlaceRepository;
import com.example.jejugilmoa.global.external.tourapi.KorServiceClient;
import com.example.jejugilmoa.global.external.tourapi.TourApiClient;
import com.example.jejugilmoa.global.external.tourapi.dto.AreaBasedItem;
import com.example.jejugilmoa.global.external.tourapi.dto.TourListItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 주간 인기도 동기화 (매주 월요일 02:00)
 * - KorService2 areaBasedList2?arrange=Q → PopularPlace.visitCount 갱신
 * - TarRlteTarService1 areaBasedList2 → TRAVELER_PICK 레이블 갱신
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PopularPlaceSyncScheduler {

    private static final int POPULARITY_FETCH_SIZE = 100;
    private static final int TRAVELER_PICK_COUNT = 20;

    private final KorServiceClient korServiceClient;
    private final TourApiClient tourApiClient;
    private final PopularPlaceRepository popularPlaceRepository;

    @Scheduled(cron = "0 0 2 * * MON", zone = "Asia/Seoul")
    @Transactional
    public void syncWeeklyPopularity() {
        log.info("주간 인기도 동기화 시작");
        try {
            syncKorServicePopularity();
        } catch (Exception e) {
            log.error("KorService2 인기도 동기화 실패", e);
        }
        try {
            syncTravellerPick();
        } catch (Exception e) {
            log.error("TRAVELER_PICK 동기화 실패", e);
        }
        log.info("주간 인기도 동기화 완료");
    }

    /**
     * KorService2 areaBasedList2?arrange=Q(추천순) → visitCount 갱신
     * 순위 1위 = 100점, 이후 1씩 감소 (최소 1점)
     */
    private void syncKorServicePopularity() {
        List<AreaBasedItem> items = korServiceClient.areaBasedListByPopularity(POPULARITY_FETCH_SIZE, 1);
        log.info("KorService2 인기 장소 조회: {}건", items.size());

        for (int i = 0; i < items.size(); i++) {
            AreaBasedItem item = items.get(i);
            if (item.contentid() == null) continue;

            int score = Math.max(1, POPULARITY_FETCH_SIZE - i);
            popularPlaceRepository.findByPlaceExternalId(item.contentid())
                    .ifPresent(pp -> {
                        pp.updateVisitCount(score);
                        if (pp.getPlace().getImageUrl() == null
                                && item.firstimage() != null && !item.firstimage().isBlank()) {
                            pp.getPlace().updateImageUrl(item.firstimage());
                        }
                    });
        }
    }

    /**
     * TarRlteTarService1 areaBasedList2 → rlteTatsCd 집계 → 빈도 상위 N개 TRAVELER_PICK 지정
     * 기존 TRAVELER_PICK 초기화 후 재지정.
     */
    private void syncTravellerPick() {
        popularPlaceRepository.clearCurationLabelByType(CurationLabel.TRAVELER_PICK);

        Map<String, Integer> frequencyMap = new HashMap<>();
        for (String signguCd : List.of(TourApiClient.SIGNGU_JEJU_SI, TourApiClient.SIGNGU_SEOGWIPO)) {
            try {
                List<TourListItem> items = tourApiClient.getAreaBased(signguCd, 1, 100);
                for (TourListItem item : items) {
                    if (item.rlteTatsCd() != null) {
                        frequencyMap.merge(item.rlteTatsCd(), 1, Integer::sum);
                    }
                }
            } catch (Exception e) {
                log.warn("TarRlteTarService1 조회 실패: signguCd={}", signguCd, e);
            }
        }

        // 빈도 상위 TRAVELER_PICK_COUNT개 contentId → TRAVELER_PICK 레이블 부여
        frequencyMap.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(TRAVELER_PICK_COUNT)
                .forEach(entry -> {
                    String contentId = entry.getKey();
                    popularPlaceRepository.findByPlaceExternalId(contentId)
                            .ifPresent(pp -> pp.updateCurationLabel(CurationLabel.TRAVELER_PICK));
                });

        log.info("TRAVELER_PICK 동기화 완료: 후보 {}건 중 최대 {}개 지정", frequencyMap.size(), TRAVELER_PICK_COUNT);
    }
}
