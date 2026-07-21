package com.example.jejugilmoa.global.scheduler;

import com.example.jejugilmoa.domain.place.service.PlaceSyncService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PlaceDataSyncSchedulerTest {

    @Mock PlaceSyncService placeSyncService;
    @InjectMocks PlaceDataSyncScheduler scheduler;

    @Test
    void syncAll_delegatesToPlaceSyncService() {
        scheduler.syncAll();
        verify(placeSyncService).syncAllCategories();
    }
}
