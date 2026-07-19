package com.example.jejugilmoa.domain.record.repository;

import com.example.jejugilmoa.domain.record.entity.TravelRecord;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TravelRecordRepository extends JpaRepository<TravelRecord, Long> {
    long countByUserIdAndDeletedAtIsNull(Long userId);
}
