package com.example.jejugilmoa.domain.recommendation.repository;

import com.example.jejugilmoa.domain.recommendation.entity.RecommendedCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecommendedCourseRepository extends JpaRepository<RecommendedCourse, Long> {

    @Query("""
            SELECT DISTINCT c FROM RecommendedCourse c
            LEFT JOIN FETCH c.paths p
            LEFT JOIN FETCH p.place
            ORDER BY c.copyCount DESC
            """)
    List<RecommendedCourse> findAllWithPathsOrderByCopyCountDesc();

    @Query("""
            SELECT DISTINCT c FROM RecommendedCourse c
            LEFT JOIN FETCH c.paths p
            LEFT JOIN FETCH p.place
            WHERE c.theme IN :themes
            ORDER BY c.copyCount DESC
            """)
    List<RecommendedCourse> findAllByThemeInWithPathsOrderByCopyCountDesc(@Param("themes") List<String> themes);
}
