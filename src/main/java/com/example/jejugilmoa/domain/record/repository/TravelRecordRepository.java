package com.example.jejugilmoa.domain.record.repository;

import com.example.jejugilmoa.domain.record.entity.TravelRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TravelRecordRepository extends JpaRepository<TravelRecord, Long> {
    long countByUserIdAndDeletedAtIsNull(Long userId);

    @Query("SELECT r.id FROM TravelRecord r WHERE r.travelPlan.id = :planId")
    List<Long> findIdsByTravelPlanId(@Param("planId") Long planId);

    @Modifying
    @Query("DELETE FROM TravelRecord r WHERE r.travelPlan.id = :planId")
    void deleteByTravelPlanId(@Param("planId") Long planId);
}
