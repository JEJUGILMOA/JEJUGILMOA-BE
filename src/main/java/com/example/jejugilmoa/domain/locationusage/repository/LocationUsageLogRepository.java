package com.example.jejugilmoa.domain.locationusage.repository;

import com.example.jejugilmoa.domain.locationusage.entity.LocationUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;

public interface LocationUsageLogRepository extends JpaRepository<LocationUsageLog, Long> {

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("DELETE FROM LocationUsageLog l WHERE l.receivedAt < :cutoff")
    long deleteByReceivedAtBefore(@Param("cutoff") Instant cutoff);
}
