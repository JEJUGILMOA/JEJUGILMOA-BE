package com.example.jejugilmoa.domain.record.repository;

import com.example.jejugilmoa.domain.record.entity.TravelRecordImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TravelRecordImageRepository extends JpaRepository<TravelRecordImage, Long> {

    @Modifying
    @Query("DELETE FROM TravelRecordImage i WHERE i.travelRecord.id IN :recordIds")
    void deleteByTravelRecordIdIn(@Param("recordIds") List<Long> recordIds);
}
