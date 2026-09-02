package com.example.jejugilmoa.domain.record.repository;

import com.example.jejugilmoa.domain.record.entity.TravelRecordPlace;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

public interface TravelRecordPlaceRepository extends JpaRepository<TravelRecordPlace, Long> {

    interface VisitedPlaceCount {
        Long getRecordId();
        Long getCount();
    }

    /**
     * width_bucket 두 번의 결과를 (row, col, 집계점수) 형태로 받기 위한 프로젝션.
     * PopularPlaceRepository에도 구조적으로 동일한 인터페이스가 별도로 존재한다 — 레포지토리가
     * 서로(or map 도메인)를 참조하지 않도록 일부러 공유 타입을 만들지 않고 각자 선언했다.
     */
    interface GridCellCount {
        Integer getGridRow();
        Integer getGridCol();
        Long getScore();
    }

    @Query("""
            SELECT rp.travelRecord.id AS recordId, COUNT(rp) AS count
            FROM TravelRecordPlace rp
            WHERE rp.travelRecord.id IN :recordIds
              AND rp.travelRecord.deletedAt IS NULL
              AND rp.visited = true
            GROUP BY rp.travelRecord.id
            """)
    List<VisitedPlaceCount> countVisitedByRecordIds(@Param("recordIds") Collection<Long> recordIds);

    @Query("""
            SELECT rp FROM TravelRecordPlace rp
            WHERE rp.travelRecord.id IN :recordIds
              AND rp.travelRecord.deletedAt IS NULL
            ORDER BY rp.travelRecord.id ASC, rp.visitDate ASC, rp.sequenceOrder ASC
            """)
    List<TravelRecordPlace> findAllByRecordIdsInSnapshotOrder(@Param("recordIds") Collection<Long> recordIds);

    @Query("""
            SELECT rp FROM TravelRecordPlace rp
            WHERE rp.travelRecord.id = :recordId
              AND rp.travelRecord.deletedAt IS NULL
            ORDER BY rp.visitDate ASC, rp.sequenceOrder ASC
            """)
    List<TravelRecordPlace> findAllByRecordIdInSnapshotOrder(@Param("recordId") Long recordId);

    /**
     * 실제 방문 기록(TravelRecordPlace) 기준 지도 뷰포트 내 혼잡도 격자 집계.
     * MapQueryService가 이 결과를 "실방문 우선" 신호로 사용하고, 방문 기록이 없는 격자에 한해
     * PopularPlaceRepository#aggregatePopularityByGrid의 결과로 보완한다.
     *
     * <p>width_bucket(value, min, max, 격자수)은 value가 속한 1..격자수 구간의 인덱스를 반환하는데,
     * value가 정확히 max와 같으면 (격자수+1) 버킷으로 튀는 특성이 있어 LEAST/GREATEST로
     * [1, gridSize] 범위에 강제로 클램프한다. latitude/longitude 컬럼이 numeric이라
     * width_bucket(numeric 버전과의 오버로드 모호성을 피하려고 ::float8로 명시 캐스팅했다.
     * TravelRecord는 소프트 삭제 엔티티이므로 tr.deleted_at IS NULL 필터가 필수다 (ADR-0003) —
     * 빠뜨리면 삭제된 여행 기록의 방문이 계속 혼잡도에 반영된다.</p>
     */
    @Query(value = """
            SELECT
                LEAST(GREATEST(width_bucket(p.latitude::float8, :minLat, :maxLat, :gridSize), 1), :gridSize) AS gridRow,
                LEAST(GREATEST(width_bucket(p.longitude::float8, :minLng, :maxLng, :gridSize), 1), :gridSize) AS gridCol,
                COUNT(*) AS score
            FROM travel_record_place trp
            JOIN place p ON p.id = trp.travel_place_id
            JOIN travel_record tr ON tr.id = trp.travel_record_id
            WHERE trp.visited = true
              AND trp.visited_at >= :since
              AND tr.deleted_at IS NULL
              AND p.is_published = true
              AND p.geom && ST_MakeEnvelope(:minLng, :minLat, :maxLng, :maxLat, 4326)
            GROUP BY gridRow, gridCol
            """, nativeQuery = true)
    List<GridCellCount> aggregateVisitsByGrid(
            @Param("minLng") double minLng,
            @Param("minLat") double minLat,
            @Param("maxLng") double maxLng,
            @Param("maxLat") double maxLat,
            @Param("gridSize") int gridSize,
            @Param("since") LocalDateTime since
    );

    @Modifying
    @Query("DELETE FROM TravelRecordPlace rp WHERE rp.travelRecord.id IN :recordIds")
    void deleteByTravelRecordIdIn(@Param("recordIds") List<Long> recordIds);
}
