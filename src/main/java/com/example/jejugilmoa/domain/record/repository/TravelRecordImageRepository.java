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
            SELECT i FROM TravelRecord r
            JOIN r.thumbnailImage i
            WHERE r.id IN :recordIds
              AND r.deletedAt IS NULL
            ORDER BY r.id ASC
            """)
    List<TravelRecordImage> findThumbnailImagesByRecordIds(@Param("recordIds") List<Long> recordIds);

    @Query("""
            SELECT i.travelRecord.id AS recordId, COUNT(i) AS count
            FROM TravelRecordImage i
            WHERE i.travelRecord.id IN :recordIds
              AND i.travelRecord.deletedAt IS NULL
            GROUP BY i.travelRecord.id
            """)
    List<RecordImageCount> countByRecordIds(@Param("recordIds") List<Long> recordIds);

    @Query("""
            SELECT i FROM TravelRecordImage i
            WHERE i.travelRecord.id = :recordId
              AND i.travelRecord.deletedAt IS NULL
            ORDER BY i.sequenceOrder ASC
            """)
    List<TravelRecordImage> findAllByRecordIdOrderBySequence(@Param("recordId") Long recordId);

    @Modifying
    @Query("DELETE FROM TravelRecordImage i WHERE i.travelRecord.id IN :recordIds")
    void deleteByTravelRecordIdIn(@Param("recordIds") List<Long> recordIds);
}
