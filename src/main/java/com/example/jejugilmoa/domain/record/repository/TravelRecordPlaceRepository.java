package com.example.jejugilmoa.domain.record.repository;

import com.example.jejugilmoa.domain.record.entity.TravelRecordPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TravelRecordPlaceRepository extends JpaRepository<TravelRecordPlace, Long> {

    @Query("""
                    SELECT COUNT(DISTINCT trp.place.id) FROM TravelRecordPlace trp
                    WHERE trp.travelRecord.user.id = :userId
                      AND trp.travelRecord.user.deletedAt IS NULL
                      AND trp.travelRecord.deletedAt IS NULL
                      AND trp.visited = true
        """)

    long countDistinctVisitedPlacesByUser(@Param("userId") Long userId);

    @Query("""
        SELECT COUNT(trp) FROM TravelRecordPlace trp
        WHERE trp.travelRecord.user.id = :userId
          AND trp.travelRecord.deletedAt IS NULL
          AND trp.travelRecord.user.deletedAt IS NULL
          AND trp.visited = true
          AND trp.place.id = :placeId
        """)
    long countVisitedByUserAndPlace(@Param("userId") Long userId, @Param("placeId") Long placeId);

    @Query("""
        SELECT COUNT(DISTINCT trp.place.id) FROM TravelRecordPlace trp
        WHERE trp.travelRecord.user.id = :userId
          AND trp.travelRecord.deletedAt IS NULL
          AND trp.travelRecord.user.deletedAt IS NULL
          AND trp.visited = true
          AND trp.place.category.id = :categoryId
        """)
    long countDistinctVisitedPlacesByUserAndCategory(@Param("userId") Long userId, @Param("categoryId") Long categoryId);

    @Query("""
        SELECT COUNT(DISTINCT trp.place.id) FROM TravelRecordPlace trp
        WHERE trp.travelRecord.user.id = :userId
          AND trp.travelRecord.deletedAt IS NULL
          AND trp.travelRecord.user.deletedAt IS NULL
          AND trp.visited = true
          AND trp.place.address LIKE CONCAT('%', :region, '%')
        """)
    long countDistinctVisitedPlacesByUserAndRegion(@Param("userId") Long userId, @Param("region") String region);
}
