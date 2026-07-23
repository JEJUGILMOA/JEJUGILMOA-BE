package com.example.jejugilmoa.domain.place.repository;

import com.example.jejugilmoa.domain.place.entity.Place;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    Optional<Place> findByExternalId(String externalId);
    Optional<Place> findByIdAndPublishedTrue(Long id);
    Page<Place> findByPublishedTrue(Pageable pageable);
    Page<Place> findByCategoryNameAndPublishedTrue(String categoryName, Pageable pageable);
    boolean existsByExternalId(String externalId);
}
