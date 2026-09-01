package com.example.jejugilmoa.domain.place.service;

import com.example.jejugilmoa.domain.place.dto.PlaceSyncResponse;
import com.example.jejugilmoa.domain.place.exception.PlaceErrorCode;
import com.example.jejugilmoa.global.apiPayload.exception.GeneralException;
import com.example.jejugilmoa.global.external.tourapi.KorServiceClient;
import com.example.jejugilmoa.global.external.tourapi.TourApiClient;
import com.example.jejugilmoa.global.external.tourapi.dto.AreaBasedItem;
import com.example.jejugilmoa.global.external.tourapi.dto.TourListItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceSyncService {

    private final TourApiClient tourApiClient;
    private final PlacePersistService placePersistService;
    private final KorServiceClient korServiceClient;

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

    private static final int BATCH_SIZE = 500;

    public void syncFromKorService() {
        List<AreaBasedItem> items = korServiceClient.areaBasedListByPopularity(1000, 1);
        log.info("KorService2 areaBasedList2 조회: {}건", items.size());
        placePersistService.saveKorServiceItems(items);
    }

    /** KorService2 areaBasedList2 기준 페이지(1-based)를 지정해 500건씩 추가 동기화. */
    public int syncBatch(int pageNo) {
        Assert.isTrue(pageNo >= 1, "pageNo는 1 이상이어야 합니다");
        log.info("장소 배치 동기화 시작: page={}, numOfRows={}", pageNo, BATCH_SIZE);
        List<AreaBasedItem> items = korServiceClient.areaBasedListByPopularity(BATCH_SIZE, pageNo);
        log.info("KorService2 areaBasedList2 조회: {}건 (page={})", items.size(), pageNo);
        int saved = placePersistService.saveKorServiceItems(items);
        log.info("장소 배치 동기화 완료: {}건 신규 저장 (중복 스킵 {}건)", saved, items.size() - saved);
        return saved;
    }
}
