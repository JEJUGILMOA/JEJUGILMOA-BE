package com.example.jejugilmoa.domain.plan.repository;

import com.example.jejugilmoa.domain.plan.entity.TravelPlan;
import com.example.jejugilmoa.domain.plan.enums.TravelPlanStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

import java.util.List;

public interface TravelPlanRepository extends JpaRepository<TravelPlan, Long> {

    @Query("SELECT p FROM TravelPlan p WHERE p.user.id = :userId AND p.user.deletedAt IS NULL ORDER BY p.createdAt DESC")
    List<TravelPlan> findMyPlans(@Param("userId") Long userId);

    @Query("SELECT p FROM TravelPlan p WHERE p.user.id = :userId AND p.user.deletedAt IS NULL AND p.status = :status ORDER BY p.createdAt DESC")
    List<TravelPlan> findMyPlansByStatus(@Param("userId") Long userId, @Param("status") TravelPlanStatus status);

    // 유저당 진행중(IN_PROGRESS) 여행은 최대 1건이라는 비즈니스 규칙을 전제로 함
    @Query("SELECT p FROM TravelPlan p WHERE p.user.id = :userId AND p.user.deletedAt IS NULL AND p.status = :status")
    Optional<TravelPlan> findByUserIdAndStatus(@Param("userId") Long userId, @Param("status") TravelPlanStatus status);

    @Query("SELECT p.id, COUNT(c) FROM TravelPlan p LEFT JOIN p.travelCourses c WHERE p.id IN :planIds GROUP BY p.id")
    List<Object[]> countCoursesByPlanIds(@Param("planIds") List<Long> planIds);

    // 재방문형 뱃지(완료한 여행 N회) 판정용
    @Query("SELECT COUNT(p) FROM TravelPlan p WHERE p.user.id = :userId AND p.user.deletedAt IS NULL AND p.status = :status")
    long countByUserIdAndStatus(@Param("userId") Long userId, @Param("status") TravelPlanStatus status);

    // 경유지 추가/삭제 시 순번 충돌 방지용 — 같은 plan에 대한 쓰기 요청을 직렬화
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM TravelPlan p WHERE p.id = :planId")
    Optional<TravelPlan> findByIdForUpdate(@Param("planId") Long planId);

    @Query("""
        SELECT p FROM TravelPlan p
                    JOIN p.user u
        LEFT JOIN FETCH p.preferredCategories
        WHERE p.id = :planId
                    AND u.deletedAt IS NULL
        """)
    Optional<TravelPlan> findByIdWithPreferences(@Param("planId") Long planId);

}
