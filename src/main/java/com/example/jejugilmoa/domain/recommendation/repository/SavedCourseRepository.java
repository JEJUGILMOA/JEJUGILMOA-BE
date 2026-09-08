package com.example.jejugilmoa.domain.recommendation.repository;

import com.example.jejugilmoa.domain.recommendation.entity.SavedCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SavedCourseRepository extends JpaRepository<SavedCourse, Long> {

    @Query("""
            SELECT sc FROM SavedCourse sc
            LEFT JOIN FETCH sc.recommendedCourse
            LEFT JOIN FETCH sc.travelRecord
            WHERE sc.user.id = :userId
            ORDER BY sc.createdAt DESC
            """)
    List<SavedCourse> findAllByUserIdWithSources(@Param("userId") Long userId);

    Optional<SavedCourse> findByUserIdAndRecommendedCourseId(Long userId, Long recommendedCourseId);

    Optional<SavedCourse> findByUserIdAndTravelRecordId(Long userId, Long travelRecordId);

    boolean existsByUserIdAndRecommendedCourseId(Long userId, Long recommendedCourseId);

    boolean existsByUserIdAndTravelRecordId(Long userId, Long travelRecordId);
}
