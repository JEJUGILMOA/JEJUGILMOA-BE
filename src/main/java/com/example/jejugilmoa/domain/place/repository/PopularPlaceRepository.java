package com.example.jejugilmoa.domain.place.repository;

import com.example.jejugilmoa.domain.place.entity.Place;
import com.example.jejugilmoa.domain.place.entity.PopularPlace;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PopularPlaceRepository extends JpaRepository<PopularPlace, Long> {
    List<PopularPlace> findAllByOrderByVisitCountDesc(Pageable pageable);
    Optional<PopularPlace> findByPlace(Place place);
}
