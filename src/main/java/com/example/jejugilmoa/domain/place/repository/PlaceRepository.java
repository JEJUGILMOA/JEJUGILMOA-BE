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
    List<Place> findByExternalIdIn(List<String> externalIds);
    Optional<Place> findByNameIgnoreCase(String name);

    @Query(value = """
            SELECT p.* FROM place p
            LEFT JOIN category c ON c.id = p.category_id
            WHERE p.is_published = true
              AND (CAST(:keyword AS text) IS NULL
                   OR lower(p.name)    LIKE lower('%' || CAST(:keyword AS text) || '%') ESCAPE '!'
                   OR lower(p.address) LIKE lower('%' || CAST(:keyword AS text) || '%') ESCAPE '!')
              AND (CAST(:categoryName AS text) IS NULL OR c.name = CAST(:categoryName AS text))
            """,
            countQuery = """
            SELECT COUNT(p.id) FROM place p
            LEFT JOIN category c ON c.id = p.category_id
            WHERE p.is_published = true
              AND (CAST(:keyword AS text) IS NULL
                   OR lower(p.name)    LIKE lower('%' || CAST(:keyword AS text) || '%') ESCAPE '!'
                   OR lower(p.address) LIKE lower('%' || CAST(:keyword AS text) || '%') ESCAPE '!')
              AND (CAST(:categoryName AS text) IS NULL OR c.name = CAST(:categoryName AS text))
            """,
            nativeQuery = true)
    Page<Place> search(@Param("keyword") String keyword, @Param("categoryName") String categoryName, Pageable pageable);

    /**
     * 출발지·목적지 좌표가 없을 때의 폴백 추천입니다.
     * 카테고리 필터 후 방문자 수(visitor_count) 내림차순으로 인기 장소를 반환합니다.
     *
     * <p>excludedIds가 빈 경우 반드시 -1L 을 포함시켜 IN 절 오류를 방지해야 합니다.</p>
     */
    @Query(value = """
            SELECT p.* FROM place p
            JOIN category c ON c.id = p.category_id
            WHERE p.is_published = true
              AND c.name IN (:categoryNames)
              AND p.id NOT IN (:excludedIds)
            ORDER BY p.visitor_count DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Place> findByCategoriesOrderByPopularity(
            @Param("categoryNames") List<String> categoryNames,
            @Param("excludedIds") List<Long> excludedIds,
            @Param("limit") int limit
    );

    /**
     * 전역 랜덤 추천. 선호경유지가 없을 때 사용합니다.
     */
    @Query(value = """
            SELECT * FROM place
            WHERE is_published = true
              AND id NOT IN (:excludedIds)
            ORDER BY RANDOM()
            LIMIT :limit
            """, nativeQuery = true)
    List<Place> findRandom(
            @Param("excludedIds") List<Long> excludedIds,
            @Param("limit") int limit
    );

    /**
     * 카테고리 필터 전역 랜덤 추천.
     */
    @Query(value = """
            SELECT p.* FROM place p
            JOIN category c ON c.id = p.category_id
            WHERE p.is_published = true
              AND c.name = :categoryName
              AND p.id NOT IN (:excludedIds)
            ORDER BY RANDOM()
            LIMIT :limit
            """, nativeQuery = true)
    List<Place> findRandomByCategory(
            @Param("categoryName") String categoryName,
            @Param("excludedIds") List<Long> excludedIds,
            @Param("limit") int limit
    );

    /**
     * 단일 앵커 추천용 반경 쿼리 (방향 기준 없음, 카테고리 필터 없음).
     * 선호경유지 1개 + 출발지 없는 경우에 부채꼴 대신 사용합니다.
     */
    @Query(value = """
            SELECT p.* FROM place p
            WHERE p.is_published = true
              AND p.id NOT IN (:excludedIds)
              AND ST_DWithin(
                  p.geom::geography,
                  ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                  :radiusMeters
              )
            ORDER BY ST_Distance(
                p.geom::geography,
                ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
            )
            LIMIT :limit
            """, nativeQuery = true)
    List<Place> findWithinRadius(
            @Param("lon") double lon,
            @Param("lat") double lat,
            @Param("radiusMeters") double radiusMeters,
            @Param("excludedIds") List<Long> excludedIds,
            @Param("limit") int limit
    );

    /**
     * 단일 앵커 추천용 반경 쿼리 (카테고리 필터 있음).
     */
    @Query(value = """
            SELECT p.* FROM place p
            JOIN category c ON c.id = p.category_id
            WHERE p.is_published = true
              AND c.name = :categoryName
              AND p.id NOT IN (:excludedIds)
              AND ST_DWithin(
                  p.geom::geography,
                  ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography,
                  :radiusMeters
              )
            ORDER BY ST_Distance(
                p.geom::geography,
                ST_SetSRID(ST_MakePoint(:lon, :lat), 4326)::geography
            )
            LIMIT :limit
            """, nativeQuery = true)
    List<Place> findWithinRadiusByCategory(
            @Param("lon") double lon,
            @Param("lat") double lat,
            @Param("radiusMeters") double radiusMeters,
            @Param("categoryName") String categoryName,
            @Param("excludedIds") List<Long> excludedIds,
            @Param("limit") int limit
    );

    /**
     * DRAFT 앵커 추천용 부채꼴 쿼리 (카테고리 필터 없음).
     * findInSector와 쿼리 구조가 동일하나 category_id 조건을 제거해
     * 선호 테마와 무관하게 인접 장소를 추천합니다.
     */
    @Query(value = """
            SELECT p.* FROM place p
            WHERE p.is_published = true
              AND p.id NOT IN (:excludedIds)
              AND ST_DWithin(
                  p.geom::geography,
                  ST_SetSRID(ST_MakePoint(:bLon, :bLat), 4326)::geography,
                  :radiusMeters
              )
              AND CASE
                    WHEN (power(ST_X(p.geom) - :bLon, 2) + power(ST_Y(p.geom) - :bLat, 2)) < 1e-18
                    THEN false
                    ELSE (
                        (:cLon - :bLon) * (ST_X(p.geom) - :bLon) +
                        (:cLat - :bLat) * (ST_Y(p.geom) - :bLat)
                    ) >= :cosHalfAngle * (
                        sqrt(power(:cLon - :bLon, 2) + power(:cLat - :bLat, 2)) *
                        sqrt(power(ST_X(p.geom) - :bLon, 2) + power(ST_Y(p.geom) - :bLat, 2))
                    )
                  END
            ORDER BY ST_Distance(
                p.geom::geography,
                ST_SetSRID(ST_MakePoint(:bLon, :bLat), 4326)::geography
            )
            LIMIT :limit
            """, nativeQuery = true)
    List<Place> findInSectorAll(
            @Param("bLon") double bLon,
            @Param("bLat") double bLat,
            @Param("cLon") double cLon,
            @Param("cLat") double cLat,
            @Param("radiusMeters") double radiusMeters,
            @Param("cosHalfAngle") double cosHalfAngle,
            @Param("excludedIds") List<Long> excludedIds,
            @Param("limit") int limit
    );

    /**
     * DRAFT 앵커 추천용 부채꼴 쿼리 (카테고리 필터 있음).
     */
    @Query(value = """
            SELECT p.* FROM place p
            JOIN category c ON c.id = p.category_id
            WHERE p.is_published = true
              AND c.name = :categoryName
              AND p.id NOT IN (:excludedIds)
              AND ST_DWithin(
                  p.geom::geography,
                  ST_SetSRID(ST_MakePoint(:bLon, :bLat), 4326)::geography,
                  :radiusMeters
              )
              AND CASE
                    WHEN (power(ST_X(p.geom) - :bLon, 2) + power(ST_Y(p.geom) - :bLat, 2)) < 1e-18
                    THEN false
                    ELSE (
                        (:cLon - :bLon) * (ST_X(p.geom) - :bLon) +
                        (:cLat - :bLat) * (ST_Y(p.geom) - :bLat)
                    ) >= :cosHalfAngle * (
                        sqrt(power(:cLon - :bLon, 2) + power(:cLat - :bLat, 2)) *
                        sqrt(power(ST_X(p.geom) - :bLon, 2) + power(ST_Y(p.geom) - :bLat, 2))
                    )
                  END
            ORDER BY ST_Distance(
                p.geom::geography,
                ST_SetSRID(ST_MakePoint(:bLon, :bLat), 4326)::geography
            )
            LIMIT :limit
            """, nativeQuery = true)
    List<Place> findInSectorAllByCategory(
            @Param("bLon") double bLon,
            @Param("bLat") double bLat,
            @Param("cLon") double cLon,
            @Param("cLat") double cLat,
            @Param("radiusMeters") double radiusMeters,
            @Param("cosHalfAngle") double cosHalfAngle,
            @Param("categoryName") String categoryName,
            @Param("excludedIds") List<Long> excludedIds,
            @Param("limit") int limit
    );

    /**
     * B→C 방향 부채꼴(sector) 범위의 장소를 추천합니다. 여행 진행 중 경유지 추천에 사용합니다.
     *
     * <p>두 가지 조건을 AND로 적용합니다.</p>
     * <ol>
     *   <li>반경 조건: B({@code bLon},{@code bLat})로부터 {@code radiusMeters} 이내 (geography 단위)</li>
     *   <li>각도 조건: B→P와 B→C 방향의 각도 ≤ arccos({@code cosHalfAngle}).
     *       내적 공식 사용 — cos(θ) = (B→C · B→P) / (|B→C| × |B→P|) ≥ cosHalfAngle.
     *       좌표계 기반 근사(경위도 degree 단위)이므로 소규모 지역(제주도 수준)에서만 사용합니다.</li>
     * </ol>
     *
     * <p>결과는 B로부터의 거리 오름차순(가까운 것 우선)으로 반환합니다.</p>
     *
     * @param cosHalfAngle 부채꼴 반각의 코사인 값 (예: cos(60°) = 0.5 → ±60° = 120° 부채꼴)
     */
    @Query(value = """
            SELECT p.* FROM place p
            JOIN category c ON c.id = p.category_id
            WHERE p.is_published = true
              AND c.name IN (:categoryNames)
              AND p.id NOT IN (:excludedIds)
              AND ST_DWithin(
                  p.geom::geography,
                  ST_SetSRID(ST_MakePoint(:bLon, :bLat), 4326)::geography,
                  :radiusMeters
              )
              AND CASE
                    WHEN (power(ST_X(p.geom) - :bLon, 2) + power(ST_Y(p.geom) - :bLat, 2)) < 1e-18
                    THEN false
                    ELSE (
                        (:cLon - :bLon) * (ST_X(p.geom) - :bLon) +
                        (:cLat - :bLat) * (ST_Y(p.geom) - :bLat)
                    ) >= :cosHalfAngle * (
                        sqrt(power(:cLon - :bLon, 2) + power(:cLat - :bLat, 2)) *
                        sqrt(power(ST_X(p.geom) - :bLon, 2) + power(ST_Y(p.geom) - :bLat, 2))
                    )
                  END
            ORDER BY ST_Distance(
                p.geom::geography,
                ST_SetSRID(ST_MakePoint(:bLon, :bLat), 4326)::geography
            )
            LIMIT :limit
            """, nativeQuery = true)
    List<Place> findInSector(
            @Param("bLon") double bLon,
            @Param("bLat") double bLat,
            @Param("cLon") double cLon,
            @Param("cLat") double cLat,
            @Param("radiusMeters") double radiusMeters,
            @Param("cosHalfAngle") double cosHalfAngle,
            @Param("categoryNames") List<String> categoryNames,
            @Param("excludedIds") List<Long> excludedIds,
            @Param("limit") int limit
    );

    /**
     * 지도 뷰포트(bounding box) 내 장소를 조회합니다. 마커 렌더링용이라 페이지네이션 대신
     * visitor_count 내림차순 정렬 후 limit으로 잘라, 뷰포트가 밀집 지역이어도 중요한 장소부터 보여줍니다.
     *
     * <p>ST_MakeEnvelope는 경도(minLng,maxLng) 우선, 위도(minLat,maxLat) 후순으로 전달합니다 (ADR-0002).
     * &&는 bbox 겹침 연산자로, {@code idx_place_geom_published} 부분 GiST 인덱스(schema.sql)를 탑니다.</p>
     */
    @Query(value = """
            SELECT p.* FROM place p
            WHERE p.is_published = true
              AND p.geom && ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
            ORDER BY p.visitor_count DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Place> findWithinBounds(
            @Param("minLng") double minLng,
            @Param("minLat") double minLat,
            @Param("maxLng") double maxLng,
            @Param("maxLat") double maxLat,
            @Param("limit") int limit
    );

    /**
     * 지도 뷰포트(bounding box) + 카테고리명 필터로 장소를 조회합니다. {@link #findWithinBounds}와
     * 별도 메서드로 나눈 이유는 네이티브 쿼리에서 "categoryName이 null이면 전체" 같은 조건부 필터를
     * SQL 트릭으로 넣는 대신, 서비스 레이어(MapQueryService)에서 명확하게 분기하기 위함
     * ({@link com.example.jejugilmoa.domain.place.service.PlaceQueryService#browse}와 동일한 패턴).
     */
    @Query(value = """
            SELECT p.* FROM place p
            JOIN category c ON c.id = p.category_id
            WHERE p.is_published = true
              AND c.name = :categoryName
              AND p.geom && ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
            ORDER BY p.visitor_count DESC
            LIMIT :limit
            """, nativeQuery = true)
    List<Place> findWithinBoundsAndCategory(
            @Param("minLng") double minLng,
            @Param("minLat") double minLat,
            @Param("maxLng") double maxLng,
            @Param("maxLat") double maxLat,
            @Param("categoryName") String categoryName,
            @Param("limit") int limit
    );

    /**
     * 주어진 좌표가 장소 반경(radiusMeters) 이내인지 확인합니다. 방문 인증(GPS 기반)에 사용.
     * ST_MakePoint는 경도(lng) 우선, 위도(lat) 후순으로 전달합니다 (ADR-0002).
     */
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM place p
                WHERE p.id = :placeId
                  AND ST_DWithin(
                      p.geom::geography,
                      ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography,
                      :radiusMeters
                  )
            )
            """, nativeQuery = true)
    boolean existsWithinDistance(
            @Param("placeId") Long placeId,
            @Param("lng") double lng,
            @Param("lat") double lat,
            @Param("radiusMeters") double radiusMeters
    );
}
