package com.example.jejugilmoa.domain.place.repository;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.entity.PopularPlace;
import com.example.jejugilmoa.domain.place.enums.CurationLabel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PopularPlaceRepository extends JpaRepository<PopularPlace, Long> {

    @Query(value = "SELECT pp FROM PopularPlace pp JOIN FETCH pp.place ORDER BY pp.visitCount DESC",
           countQuery = "SELECT COUNT(pp) FROM PopularPlace pp")
    Page<PopularPlace> findAllWithPlaceOrderByVisitCountDesc(Pageable pageable);

    @Query(value = "SELECT pp FROM PopularPlace pp JOIN FETCH pp.place pl WHERE pl.category.name = :categoryName ORDER BY pp.visitCount DESC",
           countQuery = "SELECT COUNT(pp) FROM PopularPlace pp JOIN pp.place pl WHERE pl.category.name = :categoryName")
    Page<PopularPlace> findByCategoryNameWithPlaceOrderByVisitCountDesc(@Param("categoryName") String categoryName, Pageable pageable);
    Optional<PopularPlace> findByPlace(Place place);

    @Query("SELECT pp FROM PopularPlace pp JOIN FETCH pp.place WHERE pp.curationLabel = :label ORDER BY pp.visitCount DESC")
    List<PopularPlace> findByCurationLabelWithPlace(@Param("label") CurationLabel label, Pageable pageable);

    @Query("SELECT pp FROM PopularPlace pp JOIN FETCH pp.place WHERE pp.curationLabel IS NULL ORDER BY pp.visitCount DESC")
    List<PopularPlace> findTopGeneralWithPlace(Pageable pageable);

    @Query("SELECT pp FROM PopularPlace pp WHERE pp.place.externalId = :externalId")
    Optional<PopularPlace> findByPlaceExternalId(@Param("externalId") String externalId);

    @Modifying
    @Query("UPDATE PopularPlace pp SET pp.curationLabel = NULL WHERE pp.curationLabel = :label")
    void clearCurationLabelByType(@Param("label") CurationLabel label);

    /**
     * TravelRecordPlaceRepository.GridCellCount와 구조적으로 동일하지만 별도로 선언된 프로젝션 —
     * 서로 다른 도메인의 레포지토리가 서로를 참조하지 않도록 하기 위함. Spring Data의 인터페이스
     * 프로젝션 매핑은 이름이 아니라 구조(getGridRow/getGridCol/getScore)로 동작하므로 문제 없다.
     */
    interface GridCellCount {
        Integer getGridRow();
        Integer getGridCol();
        Long getScore();
    }

    /**
     * PopularPlace.visitCount 기준 지도 뷰포트 내 혼잡도 격자 집계 — 실방문 기록이 없는 셀에서만
     * MapQueryService가 폴백으로 사용하는 보조 신호다 (검색량/누적 방문수 기반이라 "최근" 혼잡도는 아님).
     * width_bucket 클램프/캐스팅 이유는 TravelRecordPlaceRepository#aggregateVisitsByGrid 참고.
     */
    @Query(value = """
            SELECT
                LEAST(GREATEST(width_bucket(p.latitude::float8, :minLat, :maxLat, :gridSize), 1), :gridSize) AS gridRow,
                LEAST(GREATEST(width_bucket(p.longitude::float8, :minLng, :maxLng, :gridSize), 1), :gridSize) AS gridCol,
                SUM(pp.visit_count) AS score
            FROM popular_place pp
            JOIN place p ON p.id = pp.place_id
            WHERE p.is_published = true
              AND p.geom && ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
            GROUP BY gridRow, gridCol
            """, nativeQuery = true)
    List<GridCellCount> aggregatePopularityByGrid(
            @Param("minLng") double minLng,
            @Param("minLat") double minLat,
            @Param("maxLng") double maxLng,
            @Param("maxLat") double maxLat,
            @Param("gridSize") int gridSize
    );
}
