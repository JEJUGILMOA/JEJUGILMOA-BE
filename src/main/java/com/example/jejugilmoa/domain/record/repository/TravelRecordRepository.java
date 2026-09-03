package com.example.jejugilmoa.domain.record.repository;

import com.example.jejugilmoa.domain.record.entity.TravelRecord;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TravelRecordRepository extends JpaRepository<TravelRecord, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM TravelRecord r WHERE r.id = :recordId AND r.deletedAt IS NULL")
    Optional<TravelRecord> findActiveByIdForUpdate(@Param("recordId") Long recordId);

    @Query("""
            SELECT r FROM TravelRecord r
            JOIN r.user u
            WHERE r.id = :recordId
              AND u.id = :userId
              AND r.deletedAt IS NULL
              AND u.deletedAt IS NULL
            """)
    Optional<TravelRecord> findActiveOwnedRecord(
            @Param("recordId") Long recordId, @Param("userId") Long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT r FROM TravelRecord r
            JOIN r.user u
            WHERE r.id = :recordId
              AND u.id = :userId
              AND r.deletedAt IS NULL
              AND u.deletedAt IS NULL
            """)
    Optional<TravelRecord> findActiveOwnedRecordForUpdate(
            @Param("recordId") Long recordId, @Param("userId") Long userId);

    @Query("""
            SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
            FROM TravelRecord r
            JOIN r.user u
            WHERE r.id = :recordId
              AND r.deletedAt IS NULL
              AND u.deletedAt IS NULL
            """)
    boolean existsActiveById(@Param("recordId") Long recordId);

    long countByUserIdAndDeletedAtIsNull(Long userId);

    boolean existsByTravelPlanIdAndDeletedAtIsNull(Long planId);

    @Query(
            value = """
                    SELECT r FROM TravelRecord r
                    JOIN FETCH r.user u
                    WHERE r.user.id = :userId
                      AND r.deletedAt IS NULL
                      AND u.deletedAt IS NULL
                    """,
            countQuery = """
                    SELECT COUNT(r) FROM TravelRecord r
                    JOIN r.user u
                    WHERE r.user.id = :userId
                      AND r.deletedAt IS NULL
                      AND u.deletedAt IS NULL
                    """)
    Page<TravelRecord> findActiveByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query(
            value = """
                    SELECT r FROM TravelRecord r
                    JOIN FETCH r.user u
                    WHERE r.visibility = com.example.jejugilmoa.domain.plan.enums.Visibility.PUBLIC
                      AND r.deletedAt IS NULL
                      AND u.deletedAt IS NULL
                    """,
            countQuery = """
                    SELECT COUNT(r) FROM TravelRecord r
                    JOIN r.user u
                    WHERE r.visibility = com.example.jejugilmoa.domain.plan.enums.Visibility.PUBLIC
                      AND r.deletedAt IS NULL
                      AND u.deletedAt IS NULL
                    """)
    Page<TravelRecord> findActivePublic(Pageable pageable);

    @Query("""
            SELECT r FROM TravelRecord r
            JOIN FETCH r.user u
            LEFT JOIN FETCH r.travelPlan p
            LEFT JOIN FETCH r.thumbnailImage ti
            WHERE r.id = :recordId
              AND r.deletedAt IS NULL
              AND u.deletedAt IS NULL
            """)
    Optional<TravelRecord> findActiveByIdWithUserAndPlan(@Param("recordId") Long recordId);
}
