package com.example.jejugilmoa.domain.plan.repository;

import com.example.jejugilmoa.domain.plan.entity.TravelCourse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TravelCourseRepository extends JpaRepository<TravelCourse, Long> {

    interface PlaceVisitCount {
        Long getPlaceId();
        Long getCnt();
    }

    interface CategoryVisitCount {
        Long getCategoryId();
        Long getCnt();
    }

    interface PlaceAddress {
        Long getPlaceId();
        String getAddress();
    }

    interface PlaceVisitTime {
        Long getPlaceId();
        java.time.LocalDateTime getVisitedAt();
    }

    /**
     * 뱃지 진행도/지급 판정용 — 실시간 방문 인증(TravelCourse.visited) 기준으로 집계한다.
     * 건너뛴 경유지(skipped = true)는 실제 방문이 아니므로 제외한다. 여러 배지 조건에 걸친
     * place를 한 번에 IN 조회해 N+1을 방지한다.
     */
    @Query("""
            SELECT c.place.id AS placeId, COUNT(c) AS cnt FROM TravelCourse c
            WHERE c.travelPlan.user.id = :userId
              AND c.travelPlan.user.deletedAt IS NULL
              AND c.visited = true
              AND c.skipped = false
              AND c.place.id IN :placeIds
            GROUP BY c.place.id
            """)
    List<PlaceVisitCount> countVisitedByUserGroupedByPlace(
            @Param("userId") Long userId, @Param("placeIds") Collection<Long> placeIds);

    @Query("""
            SELECT c.place.category.id AS categoryId, COUNT(DISTINCT c.place.id) AS cnt FROM TravelCourse c
            WHERE c.travelPlan.user.id = :userId
              AND c.travelPlan.user.deletedAt IS NULL
              AND c.visited = true
              AND c.skipped = false
              AND c.place.category.id IN :categoryIds
            GROUP BY c.place.category.id
            """)
    List<CategoryVisitCount> countDistinctVisitedPlacesByUserGroupedByCategory(
            @Param("userId") Long userId, @Param("categoryIds") Collection<Long> categoryIds);

    @Query("""
            SELECT DISTINCT c.place.id AS placeId, c.place.address AS address FROM TravelCourse c
            WHERE c.travelPlan.user.id = :userId
              AND c.travelPlan.user.deletedAt IS NULL
              AND c.visited = true
              AND c.skipped = false
            """)
    List<PlaceAddress> findDistinctVisitedPlacesByUser(@Param("userId") Long userId);

    @Query("""
            SELECT COUNT(DISTINCT c.place.id) FROM TravelCourse c
            WHERE c.travelPlan.user.id = :userId
              AND c.travelPlan.user.deletedAt IS NULL
              AND c.visited = true
              AND c.skipped = false
            """)
    long countDistinctVisitedPlacesByUser(@Param("userId") Long userId);

    // 시간조건형(일출/일몰/별빛)·코스 완료형 뱃지 판정용 — 인증 시각까지 필요한 place들의 개별 방문 기록
    @Query("""
            SELECT c.place.id AS placeId, c.visitedAt AS visitedAt FROM TravelCourse c
            WHERE c.travelPlan.user.id = :userId
              AND c.travelPlan.user.deletedAt IS NULL
              AND c.visited = true
              AND c.skipped = false
              AND c.place.id IN :placeIds
            """)
    List<PlaceVisitTime> findVisitTimesByUserAndPlaceIds(
            @Param("userId") Long userId, @Param("placeIds") Collection<Long> placeIds);

    // 다양성 누적형 뱃지(서로 다른 카테고리 N개 방문) 판정용
    @Query("""
            SELECT COUNT(DISTINCT c.place.category.id) FROM TravelCourse c
            WHERE c.travelPlan.user.id = :userId
              AND c.travelPlan.user.deletedAt IS NULL
              AND c.visited = true
              AND c.skipped = false
            """)
    long countDistinctVisitedCategoriesByUser(@Param("userId") Long userId);

    // 선호 경유지 목록 — 앵커 추천 기준점 조회용
    List<TravelCourse> findAllByTravelPlanIdAndPreferredTrue(Long planId);

    // 방문 인증 순서 검증용 — sequenceOrder가 가장 빠른 미방문 경유지 (= 다음에 인증해야 할 경유지)
    Optional<TravelCourse> findFirstByTravelPlanIdAndVisitedFalseOrderByVisitDateAscSequenceOrderAsc(Long planId);

    // 부채꼴 추천용 — 가장 최근 방문 인증 경유지 (= B점)
    Optional<TravelCourse> findFirstByTravelPlanIdAndVisitedTrueOrderByVisitDateDescSequenceOrderDesc(Long planId);

    boolean existsByTravelPlanIdAndPlaceId(Long planId, Long placeId);

    // visitDate 기준 오름차순 정렬 — 목록 조회용
    List<TravelCourse> findAllByTravelPlanIdOrderByVisitDateAscSequenceOrderAsc(Long planId);

    // RecommendationService용 — Place fetch join으로 N+1 방지
    @Query("""
            SELECT c FROM TravelCourse c
            JOIN FETCH c.place
            WHERE c.travelPlan.id = :planId
            ORDER BY c.visitDate ASC, c.sequenceOrder ASC
            """)
    List<TravelCourse> findAllByTravelPlanIdWithPlaceOrderByVisitDateAscSequenceOrderAsc(
            @Param("planId") Long planId);

    @Query("""
            SELECT c FROM TravelCourse c
            JOIN FETCH c.place
            WHERE c.travelPlan.id = :planId AND c.visited = false
            ORDER BY c.visitDate ASC, c.sequenceOrder ASC
            """)
    Optional<TravelCourse> findFirstByTravelPlanIdAndVisitedFalseWithPlaceOrderByVisitDateAscSequenceOrderAsc(
            @Param("planId") Long planId);

    @Query("""
            SELECT c FROM TravelCourse c
            JOIN FETCH c.place
            WHERE c.travelPlan.id = :planId AND c.visited = true
            ORDER BY c.visitDate DESC, c.sequenceOrder DESC
            """)
    Optional<TravelCourse> findFirstByTravelPlanIdAndVisitedTrueWithPlaceOrderByVisitDateDescSequenceOrderDesc(
            @Param("planId") Long planId);

    // 특정 Day의 경유지만 조회 — 재정렬 유효성 검사용
    List<TravelCourse> findAllByTravelPlanIdAndVisitDateOrderBySequenceOrderAsc(Long planId, LocalDate visitDate);

    // 특정 Day의 경유지 수 — addWaypoint 시 nextOrder 계산용
    int countByTravelPlanIdAndVisitDate(Long planId, LocalDate visitDate);

    // removeWaypoint: 같은 Day에서 제거된 순번 이후 순번을 1씩 당김
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE TravelCourse c
            SET c.sequenceOrder = c.sequenceOrder - 1
            WHERE c.travelPlan.id = :planId
              AND c.visitDate = :visitDate
              AND c.sequenceOrder > :removedOrder
            """)
    void decrementSequenceOrderAfter(
            @Param("planId") Long planId,
            @Param("visitDate") LocalDate visitDate,
            @Param("removedOrder") int removedOrder);

    // reorderWaypoints Phase-1: 같은 Day의 순번을 임시 offset으로 이동 (uk_course_plan_date_sequence 충돌 방지)
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            UPDATE TravelCourse c
            SET c.sequenceOrder = c.sequenceOrder + :offset
            WHERE c.travelPlan.id = :planId
              AND c.visitDate = :visitDate
            """)
    void shiftSequenceOrderByOffset(
            @Param("planId") Long planId,
            @Param("visitDate") LocalDate visitDate,
            @Param("offset") int offset);

    // reorderWaypoints Phase-2: 특정 경유지의 순번을 최종값으로 확정
    @Modifying
    @Query("""
            UPDATE TravelCourse c
            SET c.sequenceOrder = :newOrder
            WHERE c.id = :waypointId
              AND c.travelPlan.id = :planId
            """)
    void updateSequenceOrder(
            @Param("waypointId") Long waypointId,
            @Param("planId") Long planId,
            @Param("newOrder") int newOrder);
}
