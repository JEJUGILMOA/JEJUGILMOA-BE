package com.example.jejugilmoa.domain.place.repository;

import com.example.jejugilmoa.domain.place.entity.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    Optional<Place> findByExternalId(String externalId);
    Optional<Place> findByIdAndPublishedTrue(Long id);
    Page<Place> findByPublishedTrue(Pageable pageable);
    Page<Place> findByCategoryNameAndPublishedTrue(String categoryName, Pageable pageable);
    boolean existsByExternalId(String externalId);

    /**
     * 출발지-목적지 직선 경로(corridor) 주변의 장소를 추천합니다.
     *
     * <p>ST_MakeLine으로 출발지→목적지 직선 선분을 만들고 ::geography 캐스트로
     * ST_DWithin에 미터 단위 반경(corridorWidthMeters)을 적용합니다.
     * ST_MakePoint는 경도(lng) 우선, 위도(lat) 후순으로 전달해야 합니다 (ADR-0002).</p>
     *
     * <p>ST_LineLocatePoint(line, point)는 선분 위 투영 위치(0~1)를 반환하므로
     * 경유지를 출발지→목적지 방향으로 정렬할 수 있습니다.
     * 이 함수는 geography가 아닌 geometry를 받으므로 캐스트 없이 사용합니다.</p>
     *
     * <p>excludedIds가 빈 경우 반드시 -1L 을 포함시켜 IN 절 오류를 방지해야 합니다.</p>
     *
     * @param deptLng             출발지 경도
     * @param deptLat             출발지 위도
     * @param destLng             목적지 경도
     * @param destLat             목적지 위도
     * @param corridorWidthMeters 경로 좌우 허용 폭 (미터)
     * @param categoryIds         선호 카테고리 ID 목록 (최소 1개)
     * @param excludedIds         제외할 장소 ID 목록 (이미 추가됐거나 건너뛴 장소, 최소 -1L)
     * @param limit               최대 반환 개수
     */
    @Query(value = """
            SELECT p.* FROM place p
            WHERE p.is_published = true
              AND p.category_id IN (:categoryIds)
              AND p.id NOT IN (:excludedIds)
              AND ST_DWithin(
                  p.geom::geography,
                  ST_MakeLine(
                      ST_SetSRID(ST_MakePoint(:deptLng, :deptLat), 4326),
                      ST_SetSRID(ST_MakePoint(:destLng, :destLat), 4326)
                  )::geography,
                  :corridorWidthMeters
              )
            ORDER BY ST_LineLocatePoint(
                ST_MakeLine(
                    ST_SetSRID(ST_MakePoint(:deptLng, :deptLat), 4326),
                    ST_SetSRID(ST_MakePoint(:destLng, :destLat), 4326)
                ),
                p.geom
            ) ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<Place> findAlongCorridor(
            @Param("deptLng") double deptLng,
            @Param("deptLat") double deptLat,
            @Param("destLng") double destLng,
            @Param("destLat") double destLat,
            @Param("corridorWidthMeters") double corridorWidthMeters,
            @Param("categoryIds") List<Long> categoryIds,
            @Param("excludedIds") List<Long> excludedIds,
            @Param("limit") int limit
    );

    /**
     * 출발지·목적지 좌표가 없을 때의 폴백 추천입니다.
     * 카테고리 필터 후 방문자 수(visitor_count) 내림차순으로 인기 장소를 반환합니다.
     *
     * <p>excludedIds가 빈 경우 반드시 -1L 을 포함시켜 IN 절 오류를 방지해야 합니다.</p>
     */
    @Query(value = """
            SELECT p.* FROM place p
            WHERE p.is_published = true
              AND p.category_id IN (:categoryIds)
              AND p.id NOT IN (:excludedIds)
            ORDER BY p.visitor_count DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Place> findByCategoriesOrderByPopularity(
            @Param("categoryIds") List<Long> categoryIds,
            @Param("excludedIds") List<Long> excludedIds,
            @Param("limit") int limit
    );
}
