package com.example.jejugilmoa.domain.record.repository;

import com.example.jejugilmoa.domain.record.entity.TravelRecord;
import com.example.jejugilmoa.domain.record.entity.TravelRecordFavorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

public interface TravelRecordFavoriteRepository extends JpaRepository<TravelRecordFavorite, Long> {
    boolean existsByUserIdAndTravelRecordId(Long userId, Long recordId);

    @Modifying
    @Query("DELETE FROM TravelRecordFavorite f WHERE f.user.id = :userId AND f.travelRecord.id = :recordId")
    int deleteByOwnerAndRecord(@Param("userId") Long userId, @Param("recordId") Long recordId);

    @Query(value = """
            SELECT r FROM TravelRecordFavorite f
            JOIN f.travelRecord r
            JOIN FETCH r.user u
            WHERE f.user.id = :userId
              AND f.user.deletedAt IS NULL
              AND r.deletedAt IS NULL
              AND u.deletedAt IS NULL
              AND r.visibility = com.example.jejugilmoa.domain.plan.enums.Visibility.PUBLIC
              AND NOT EXISTS (
                  SELECT b.id FROM UserBlock b
                  WHERE (b.blocker.id = :userId AND b.blocked.id = u.id)
                     OR (b.blocked.id = :userId AND b.blocker.id = u.id)
              )
            ORDER BY f.createdAt DESC, f.id DESC
            """, countQuery = """
            SELECT COUNT(f) FROM TravelRecordFavorite f
            JOIN f.travelRecord r
            JOIN r.user u
            WHERE f.user.id = :userId
              AND f.user.deletedAt IS NULL
              AND r.deletedAt IS NULL
              AND u.deletedAt IS NULL
              AND r.visibility = com.example.jejugilmoa.domain.plan.enums.Visibility.PUBLIC
              AND NOT EXISTS (
                  SELECT b.id FROM UserBlock b
                  WHERE (b.blocker.id = :userId AND b.blocked.id = u.id)
                     OR (b.blocked.id = :userId AND b.blocker.id = u.id)
              )
            """)
    Page<TravelRecord> findVisibleRecords(@Param("userId") Long userId, Pageable pageable);
}
