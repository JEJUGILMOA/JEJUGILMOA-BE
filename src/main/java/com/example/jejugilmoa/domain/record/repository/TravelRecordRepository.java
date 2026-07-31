package com.example.jejugilmoa.domain.record.repository;

import com.example.jejugilmoa.domain.record.entity.TravelRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TravelRecordRepository extends JpaRepository<TravelRecord, Long> {
    long countByUserIdAndDeletedAtIsNull(Long userId);

    // 플랜 hard-delete 정리용 — 소프트 삭제된 기록도 포함해야 고아 데이터가 남지 않음
    @Query("SELECT r.id FROM TravelRecord r WHERE r.travelPlan.id = :planId")
    List<Long> findIdsByTravelPlanId(@Param("planId") Long planId);

    @Modifying
    @Query("DELETE FROM TravelRecord r WHERE r.travelPlan.id = :planId")
    void deleteByTravelPlanId(@Param("planId") Long planId);
}
