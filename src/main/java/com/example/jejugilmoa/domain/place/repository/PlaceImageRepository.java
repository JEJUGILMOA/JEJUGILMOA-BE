package com.example.jejugilmoa.domain.place.repository;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.entity.PlaceImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface PlaceImageRepository extends JpaRepository<PlaceImage, Long> {
    List<PlaceImage> findByPlace_IdIn(Collection<Long> placeIds);
    boolean existsByPlace(Place place);
    void deleteByPlace(Place place);

    @Modifying
    @Query(value = """
            INSERT INTO place_image (place_id, image_url, sequence_order, created_at, updated_at)
            VALUES (:placeId, :imageUrl, :sequenceOrder, NOW(), NOW())
            ON CONFLICT (place_id, sequence_order) DO NOTHING
            """, nativeQuery = true)
    void insertIgnore(@Param("placeId") Long placeId,
                      @Param("imageUrl") String imageUrl,
                      @Param("sequenceOrder") int sequenceOrder);
}
