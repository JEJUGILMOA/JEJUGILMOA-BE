package com.example.jejugilmoa.domain.recommendation.repository;

import com.example.jejugilmoa.domain.recommendation.entity.RecommendedCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RecommendedCourseRepository extends JpaRepository<RecommendedCourse, Long> {

    interface CoursePathCount {
        Long getCourseId();
        Long getCount();
    }

    @Modifying
    @Query("UPDATE RecommendedCourse rc SET rc.copyCount = rc.copyCount + 1 WHERE rc.id = :id")
    void incrementCopyCount(@Param("id") Long id);

    @Modifying
    @Query("UPDATE RecommendedCourse rc SET rc.copyCount = CASE WHEN rc.copyCount > 0 THEN rc.copyCount - 1 ELSE 0 END WHERE rc.id = :id")
    void decrementCopyCount(@Param("id") Long id);

    @Query("""
            SELECT rcp.course.id AS courseId, COUNT(rcp) AS count
            FROM RecommendedCoursePath rcp
            WHERE rcp.course.id IN :courseIds
            GROUP BY rcp.course.id
            """)
    List<CoursePathCount> countPathsByCourseIds(@Param("courseIds") Collection<Long> courseIds);

    @Query("""
            SELECT DISTINCT c FROM RecommendedCourse c
            LEFT JOIN FETCH c.paths p
            LEFT JOIN FETCH p.place
            WHERE c.id = :courseId
            """)
    Optional<RecommendedCourse> findByIdWithPaths(@Param("courseId") Long courseId);

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
    @Query("""
            SELECT DISTINCT c FROM RecommendedCourse c
            LEFT JOIN FETCH c.paths p
            LEFT JOIN FETCH p.place
            WHERE c.id IN :courseIds
            """)
    List<RecommendedCourse> findAllByIdInWithPaths(@Param("courseIds") Collection<Long> courseIds);
}
