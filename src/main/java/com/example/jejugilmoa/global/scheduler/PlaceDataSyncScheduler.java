package com.example.jejugilmoa.global.scheduler;

import com.example.jejugilmoa.domain.place.service.PlaceSyncService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class PlaceDataSyncScheduler {

    private final PlaceSyncService placeSyncService;

    @Value("${app.sync.run-on-startup:false}")
    private boolean runOnStartup;

    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void syncAll() {
        log.info("장소 데이터 동기화 시작");
        placeSyncService.syncFromKorService();
        log.info("장소 데이터 동기화 완료");
    }

    public void enrichDetails() {
        log.info("장소 상세 정보 보강 시작");
        placeSyncService.enrichPlaceDetails();
        log.info("장소 상세 정보 보강 완료");
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onStartup() {
        if (runOnStartup) {
            log.info("앱 기동 시 장소 데이터 동기화 실행");
            try {
                syncAll();
            } catch (Exception e) {
                log.warn("기동 시 장소 데이터 동기화 실패 — 서버는 계속 가동됩니다.", e);
            }
        }
    }
}
