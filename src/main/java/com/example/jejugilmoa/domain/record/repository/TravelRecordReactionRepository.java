package com.example.jejugilmoa.domain.record.repository;

import com.example.jejugilmoa.domain.record.entity.TravelRecordReaction;
import com.example.jejugilmoa.domain.record.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TravelRecordReactionRepository extends JpaRepository<TravelRecordReaction, Long> {

    interface ReactionCount {
        Long getRecordId();
        ReactionType getReactionType();
        Long getCount();
    }

    @Query("""
            SELECT r.travelRecord.id AS recordId, r.reactionType AS reactionType, COUNT(r) AS count
            FROM TravelRecordReaction r
            WHERE r.travelRecord.id IN :recordIds
              AND r.travelRecord.deletedAt IS NULL
            GROUP BY r.travelRecord.id, r.reactionType
            """)
    List<ReactionCount> countByRecordIdsAndType(@Param("recordIds") List<Long> recordIds);

    @Query("""
            SELECT r FROM TravelRecordReaction r
            WHERE r.travelRecord.id IN :recordIds
              AND r.user.id = :userId
              AND r.travelRecord.deletedAt IS NULL
              AND r.user.deletedAt IS NULL
            """)
    List<TravelRecordReaction> findMineByRecordIds(
            @Param("recordIds") List<Long> recordIds,
            @Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM TravelRecordReaction r WHERE r.travelRecord.id IN :recordIds")
    void deleteByTravelRecordIdIn(@Param("recordIds") List<Long> recordIds);
}
