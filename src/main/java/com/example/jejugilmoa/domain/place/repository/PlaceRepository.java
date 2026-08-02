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

    @Query("SELECT p FROM Place p WHERE p.published = true " +
           "AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\' OR LOWER(p.address) LIKE LOWER(CONCAT('%', :keyword, '%')) ESCAPE '\\') " +
           "AND (:categoryName IS NULL OR p.category.name = :categoryName)")
    Page<Place> search(@Param("keyword") String keyword, @Param("categoryName") String categoryName, Pageable pageable);

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
     * 주어진 좌표가 장소 반경(radiusMeters) 이내인지 확인합니다. 방문 체크(GPS 인증)에 사용.
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
