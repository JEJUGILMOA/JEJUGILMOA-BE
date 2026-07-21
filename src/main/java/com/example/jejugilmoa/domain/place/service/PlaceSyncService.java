package com.example.jejugilmoa.domain.place.service;

import com.example.jejugilmoa.global.external.tourapi.TourApiClient;
import com.example.jejugilmoa.global.external.tourapi.dto.TourListItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaceSyncService {

    private final TourApiClient tourApiClient;
    private final PlacePersistService placePersistService;

    public void syncAllCategories() {
        List.of(TourApiClient.SIGNGU_JEJU_SI, TourApiClient.SIGNGU_SEOGWIPO)
            .forEach(signguCd -> {
                try {
                    syncBySigngu(signguCd);
                } catch (Exception e) {
                    log.error("시군구 동기화 실패: signguCd={}", signguCd, e);
                }
            });
    }

    public void syncBySigngu(String signguCd) {
        List<TourListItem> items = tourApiClient.getAreaBased(signguCd, 1, 100);
        log.info("TourAPI 동기화: signguCd={}, 결과수={}", signguCd, items.size());
        placePersistService.saveItems(signguCd, items);
    }
}
