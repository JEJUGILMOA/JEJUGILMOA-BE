package com.example.jejugilmoa.domain.place.service;

import com.example.jejugilmoa.domain.place.dto.PlaceSyncResponse;
import com.example.jejugilmoa.domain.place.entity.Place;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

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

    @Transactional
    public void enrichPlaceDetails() {
        List<Place> places = placeRepository.findByDescriptionIsNull();
        log.info("상세 정보 보강 대상: {}건", places.size());
        int count = 0;
        for (Place place : places) {
            if (place.getExternalId() == null) continue;
            try {
                DetailCommonItem common = korServiceClient.detailCommon2(place.getExternalId());
                if (common != null) {
                    place.updateCommonInfo(common.overview());
                }
                count++;
                if (count % 50 == 0) {
                    Thread.sleep(100);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("enrichPlaceDetails 중단됨");
                break;
            } catch (Exception e) {
                log.warn("상세 정보 보강 실패: contentId={}", place.getExternalId(), e);
            }
        }
        log.info("상세 정보 보강 완료: {}건 처리", count);
    }
}
