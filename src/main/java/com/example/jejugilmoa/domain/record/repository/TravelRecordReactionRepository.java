package com.example.jejugilmoa.domain.record.repository;

import com.example.jejugilmoa.domain.record.entity.TravelRecordReaction;
import com.example.jejugilmoa.domain.record.enums.ReactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface TravelRecordReactionRepository extends JpaRepository<TravelRecordReaction, Long> {

    @Query("""
            SELECT r FROM TravelRecordReaction r
            WHERE r.travelRecord.id = :recordId
              AND r.user.id = :userId
              AND r.travelRecord.deletedAt IS NULL
              AND r.user.deletedAt IS NULL
            """)
    Optional<TravelRecordReaction> findActiveByTravelRecordIdAndUserId(
            @Param("recordId") Long recordId,
            @Param("userId") Long userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            INSERT INTO record_reaction (travel_record_id, user_id, reaction_type)
            VALUES (:recordId, :userId, :reactionType)
            ON CONFLICT (travel_record_id, user_id)
            DO UPDATE SET reaction_type = EXCLUDED.reaction_type
            """, nativeQuery = true)
    void upsertReaction(
            @Param("recordId") Long recordId,
            @Param("userId") Long userId,
            @Param("reactionType") String reactionType);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query(value = """
            DELETE FROM record_reaction r
            USING travel_record tr, "user" u
            WHERE r.travel_record_id = :recordId
              AND r.user_id = :userId
              AND tr.id = r.travel_record_id
              AND u.id = r.user_id
              AND tr.deleted_at IS NULL
              AND u.deleted_at IS NULL
            """, nativeQuery = true)
    int deleteActiveByTravelRecordIdAndUserId(
            @Param("recordId") Long recordId,
            @Param("userId") Long userId);

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
