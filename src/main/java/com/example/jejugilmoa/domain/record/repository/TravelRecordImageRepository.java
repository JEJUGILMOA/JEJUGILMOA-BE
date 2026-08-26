package com.example.jejugilmoa.domain.record.repository;

import com.example.jejugilmoa.domain.record.entity.TravelRecordImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TravelRecordImageRepository extends JpaRepository<TravelRecordImage, Long> {

    interface RecordImageCount {
        Long getRecordId();
        Long getCount();
    }

    @Query("""
            SELECT i FROM TravelRecordImage i
            WHERE i.travelRecord.id IN :recordIds
              AND NOT EXISTS (
                  SELECT earlier.id FROM TravelRecordImage earlier
                  WHERE earlier.travelRecord.id = i.travelRecord.id
                    AND earlier.sequenceOrder < i.sequenceOrder
              )
            ORDER BY i.travelRecord.id ASC
            """)
    List<TravelRecordImage> findFirstImagesByRecordIds(@Param("recordIds") List<Long> recordIds);

    @Query("""
            SELECT i.travelRecord.id AS recordId, COUNT(i) AS count
            FROM TravelRecordImage i
            WHERE i.travelRecord.id IN :recordIds
            GROUP BY i.travelRecord.id
            """)
    List<RecordImageCount> countByRecordIds(@Param("recordIds") List<Long> recordIds);

    @Query("""
            SELECT i FROM TravelRecordImage i
            WHERE i.travelRecord.id = :recordId
            ORDER BY i.sequenceOrder ASC
            """)
    List<TravelRecordImage> findAllByRecordIdOrderBySequence(@Param("recordId") Long recordId);

    @Modifying
    @Query("DELETE FROM TravelRecordImage i WHERE i.travelRecord.id IN :recordIds")
    void deleteByTravelRecordIdIn(@Param("recordIds") List<Long> recordIds);
}
