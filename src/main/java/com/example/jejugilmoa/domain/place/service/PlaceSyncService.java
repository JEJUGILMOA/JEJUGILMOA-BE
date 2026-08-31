package com.example.jejugilmoa.domain.place.service;

import com.example.jejugilmoa.domain.place.dto.PlaceSyncResponse;
import com.example.jejugilmoa.domain.place.exception.PlaceErrorCode;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.example.jejugilmoa.global.external.tourapi.KorServiceClient;
import com.example.jejugilmoa.global.external.tourapi.TourApiClient;
import com.example.jejugilmoa.global.external.tourapi.dto.AreaBasedItem;
import com.example.jejugilmoa.global.external.tourapi.dto.DetailCommonItem;
import com.example.jejugilmoa.global.external.tourapi.dto.TourListItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceSyncService {

    private final TourApiClient tourApiClient;
    private final PlacePersistService placePersistService;
    private final KorServiceClient korServiceClient;
    private final PlaceRepository placeRepository;

    public PlaceSyncResponse syncAllCategories() {
        List<String> signgus = List.of(TourApiClient.SIGNGU_JEJU_SI, TourApiClient.SIGNGU_SEOGWIPO);
        List<PlaceSyncResponse.SignguSyncResult> results = new ArrayList<>();

        for (String signguCd : signgus) {
            try {
                syncBySigngu(signguCd);
                results.add(new PlaceSyncResponse.SignguSyncResult(signguCd, true, null));
            } catch (Exception e) {
                log.error("시군구 동기화 실패: signguCd={}", signguCd, e);
                results.add(new PlaceSyncResponse.SignguSyncResult(signguCd, false, e.getMessage()));
            }
        }

        int succeeded = (int) results.stream().filter(PlaceSyncResponse.SignguSyncResult::succeeded).count();
        int failed = results.size() - succeeded;

        if (succeeded == 0) {
            throw new GeneralException(PlaceErrorCode.SYNC_ALL_FAILED);
        }

        return new PlaceSyncResponse(succeeded, failed, results);
    }

    public void syncBySigngu(String signguCd) {
        List<TourListItem> items = tourApiClient.getAreaBased(signguCd, 1, 100);
        log.info("TourAPI 동기화: signguCd={}, 결과수={}", signguCd, items.size());
        placePersistService.saveItems(signguCd, items);
    }

    public void syncFromKorService() {
        List<AreaBasedItem> items = korServiceClient.areaBasedListByPopularity(1000);
        log.info("KorService2 areaBasedList2 조회: {}건", items.size());
        placePersistService.saveKorServiceItems(items);
    }

    private static final int ENRICH_BATCH_SIZE = 50;
    private static final long ENRICH_CALL_DELAY_MS = 200; // 초당 5건 — KorService2 rate limit 대비

    public void enrichPlaceDetails(int maxCalls) {
        long remaining = placeRepository.countNeedingEnrichment();
        List<String> target = placeRepository.findExternalIdsNeedingEnrichment(PageRequest.of(0, maxCalls));
        log.info("상세 정보 보강 대상: {}건 중 오늘 {}건 처리", remaining, target.size());

        int totalCount = 0;
        int totalBatches = (target.size() + ENRICH_BATCH_SIZE - 1) / ENRICH_BATCH_SIZE;

        outer:
        for (int i = 0; i < target.size(); i += ENRICH_BATCH_SIZE) {
            List<String> batch = target.subList(i, Math.min(i + ENRICH_BATCH_SIZE, target.size()));

            Map<String, String> overviews = new HashMap<>();
            for (String externalId : batch) {
                try {
                    DetailCommonItem common = korServiceClient.detailCommon2(externalId);
                    if (common != null && common.overview() != null && !common.overview().isBlank()) {
                        overviews.put(externalId, common.overview());
                    }
                } catch (Exception e) {
                    log.warn("상세 정보 보강 실패: contentId={}", externalId, e);
                }
                try {
                    Thread.sleep(ENRICH_CALL_DELAY_MS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    log.warn("enrichPlaceDetails 중단됨");
                    break outer;
                }
            }

            int batchNo = (i / ENRICH_BATCH_SIZE) + 1;
            if (!overviews.isEmpty()) {
                int saved = placePersistService.applyOverviews(overviews);
                totalCount += saved;
                log.info("상세 정보 보강 진행: 배치 {}/{}, 이번 {}건 저장 (누적 {}건)", batchNo, totalBatches, saved, totalCount);
            } else {
                log.debug("상세 정보 보강 배치 {}/{}: overview 없음, 저장 스킵", batchNo, totalBatches);
            }
        }

        long leftover = remaining - target.size();
        log.info("상세 정보 보강 완료: {}건 처리{}",
                totalCount, leftover > 0 ? " (미완료 " + leftover + "건, 내일 이어서 처리)" : "");
    }
}
