package com.example.jejugilmoa.global.scheduler;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.enums.CurationLabel;
import com.example.jejugilmoa.domain.place.repository.PlaceRepository;
import com.example.jejugilmoa.domain.place.repository.PopularPlaceRepository;
import com.example.jejugilmoa.global.external.tats.TatsCnctrRateClient;
import com.example.jejugilmoa.global.external.tats.TatsCnctrRateItem;
import com.example.jejugilmoa.global.external.tats.TatsCnctrRateProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * 일간 오늘의 픽 동기화 (매일 06:00)
 * TatsCnctrRateService tatsCnctrRatedList → cnctrRate 상위 장소 → TODAY_PICK 레이블
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TodayPickSyncScheduler {

    private static final int TODAY_PICK_COUNT = 10;
    private static final int FETCH_SIZE = 200;

    private final TatsCnctrRateClient tatsCnctrRateClient;
    private final TatsCnctrRateProperties tatsCnctrRateProperties;
    private final PopularPlaceRepository popularPlaceRepository;
    private final PlaceRepository placeRepository;

    @Scheduled(cron = "0 0 6 * * *", zone = "Asia/Seoul")
    @Transactional
    @CacheEvict(value = "popularPlaces", allEntries = true)
    public void syncTodayPick() {
        log.info("오늘의 픽 동기화 시작");

        List<String> signguCds = tatsCnctrRateProperties.signguCds();
        if (signguCds == null || signguCds.isEmpty()) {
            log.warn("TatsCnctrRateService signguCds 미설정 — TODAY_PICK 동기화 건너뜀");
            return;
        }

        // 기존 TODAY_PICK 초기화
        popularPlaceRepository.clearCurationLabelByType(CurationLabel.TODAY_PICK);

        String today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        List<TatsCnctrRateItem> allItems = new ArrayList<>();

        for (String signguCd : signguCds) {
            List<TatsCnctrRateItem> items = tatsCnctrRateClient.tatsCnctrRatedList(signguCd, FETCH_SIZE);
            // 오늘 날짜의 예측값만 사용 (없으면 가장 가까운 미래 날짜)
            List<TatsCnctrRateItem> todayItems = items.stream()
                    .filter(i -> today.equals(i.baseYmd()))
                    .toList();
            allItems.addAll(todayItems.isEmpty() ? items.stream().limit(10).toList() : todayItems);
        }

        // cnctrRate 내림차순 정렬 후 상위 TODAY_PICK_COUNT개 처리
        allItems.stream()
                .sorted(Comparator.comparingDouble(
                        (TatsCnctrRateItem i) -> parseCnctrRate(i.cnctrRate())).reversed())
                .limit(TODAY_PICK_COUNT)
                .forEach(item -> matchAndLabel(item.tAtsNm()));

        log.info("오늘의 픽 동기화 완료: 후보 {}건 처리", allItems.size());
    }

    private void matchAndLabel(String tAtsNm) {
        if (tAtsNm == null || tAtsNm.isBlank()) return;

        // 1차: 정확한 이름 매칭
        Optional<Place> matched = placeRepository.findByNameIgnoreCase(tAtsNm.trim());

        // 2차: 이름이 없으면 warn 로그만 (퍼지 매칭은 성능 이슈로 제외)
        if (matched.isEmpty()) {
            log.debug("TODAY_PICK 매칭 실패 (DB에 없는 장소): {}", tAtsNm);
            return;
        }

        popularPlaceRepository.findByPlace(matched.get())
                .ifPresent(pp -> pp.updateCurationLabel(CurationLabel.TODAY_PICK));
    }

    private double parseCnctrRate(String rate) {
        if (rate == null || rate.isBlank()) return 0.0;
        try {
            return Double.parseDouble(rate);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
